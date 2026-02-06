#!/usr/bin/env python3
"""
GPX Interval Analyzer

Analyzes GPX files to identify intervals between waypoints and detect anomalies:
- Missing intervals (large gaps between waypoints)
- Inconsistent interval patterns

Usage:
    python gpx_analyzer.py <path_to_gpx_file> [options]
    python gpx_analyzer.py <directory> [--recursive] [options]

Examples:
    python gpx_analyzer.py track.gpx
    python gpx_analyzer.py ../examples/ --recursive --threshold 120
"""

import argparse
import sys
import os
from pathlib import Path
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import List, Optional, Tuple
from xml.etree import ElementTree as ET
import statistics as stats_module
import math


@dataclass
class Waypoint:
    """Represents a single GPS waypoint from a GPX file."""
    lat: float
    lon: float
    elevation: float
    timestamp: datetime
    accuracy: Optional[float] = None
    speed: Optional[float] = None
    
    def __repr__(self) -> str:
        return f"Waypoint(lat={self.lat:.6f}, lon={self.lon:.6f}, time={self.timestamp.isoformat()})"


@dataclass
class IntervalAnalysis:
    """Analysis results for a single interval between waypoints."""
    from_waypoint: Waypoint
    to_waypoint: Waypoint
    interval_seconds: float
    distance_meters: float
    speed_ms: float
    is_anomaly: bool = False
    anomaly_type: Optional[str] = None


@dataclass
class GpxAnalysisResult:
    """Complete analysis result for a GPX file."""
    filename: str
    total_waypoints: int
    total_duration_seconds: float
    intervals: List[IntervalAnalysis] = field(default_factory=list)
    anomalies: List[IntervalAnalysis] = field(default_factory=list)
    statistics: dict = field(default_factory=dict)


def parse_gpx_file(filepath: str) -> List[Waypoint]:
    """
    Parse a GPX file and extract all waypoints.
    
    Handles GPX 1.1 format with extensions (speed, accuracy, etc.)
    """
    waypoints = []
    
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
    except ET.ParseError as e:
        raise ValueError(f"Failed to parse GPX file: {e}")
    except FileNotFoundError:
        raise ValueError(f"File not found: {filepath}")
    
    # Define namespaces (both variations with and without trailing slash)
    ns_variations = [
        'http://www.topografix.com/GPX/1/1',   # Without trailing slash
        'http://www.topografix.com/GPX/1/1/',  # With trailing slash
    ]
    
    ns = {
        'gpx': 'http://www.topografix.com/GPX/1/1',
        'gpxtpx': 'http://www.garmin.com/xmlschemas/TrackPointExtension/v1'
    }
    
    # Find all track points (try all namespace variations)
    trkpts = []
    for ns_url in ns_variations:
        trkpts = root.findall(f'.//{{{ns_url}}}trkpt')
        if trkpts:
            # Update ns dict for subsequent queries
            ns['gpx'] = ns_url
            break
    
    if not trkpts:
        trkpts = root.findall('.//trkpt')
    
    for trkpt in trkpts:
        # Get lat/lon attributes
        lat = float(trkpt.get('lat', 0))
        lon = float(trkpt.get('lon', 0))
        
        # Get elevation
        ele_elem = trkpt.find('gpx:ele', ns)
        if ele_elem is None:
            ele_elem = trkpt.find('.//{http://www.topografix.com/GPX/1/1}ele')
        if ele_elem is None:
            ele_elem = trkpt.find('ele')
        elevation = float(ele_elem.text) if ele_elem is not None and ele_elem.text else 0.0
        
        # Get timestamp
        time_elem = trkpt.find('gpx:time', ns)
        if time_elem is None:
            time_elem = trkpt.find('.//{http://www.topografix.com/GPX/1/1}time')
        if time_elem is None:
            time_elem = trkpt.find('time')
        timestamp = parse_timestamp(time_elem.text) if time_elem is not None and time_elem.text else None
        
        if timestamp is None:
            continue  # Skip waypoints without timestamps
        
        # Get extensions (speed, accuracy, etc.)
        accuracy = None
        speed = None
        
        extensions = trkpt.find('gpx:extensions', ns)
        if extensions is None:
            extensions = trkpt.find('.//{http://www.topografix.com/GPX/1/1}extensions')
        if extensions is None:
            extensions = trkpt.find('extensions')
            
        if extensions is not None:
            # Try various extension formats
            for child in extensions:
                tag = child.tag.lower()
                if 'accuracy' in tag:
                    accuracy = float(child.text) if child.text else None
                elif 'speed' in tag and 'speed' != tag:
                    speed = float(child.text) if child.text else None
                elif child.tag == 'speed' or tag.endswith('speed'):
                    speed = float(child.text) if child.text else None
        
        waypoints.append(Waypoint(
            lat=lat,
            lon=lon,
            elevation=elevation,
            timestamp=timestamp,
            accuracy=accuracy,
            speed=speed
        ))
    
    # Sort by timestamp to ensure proper order
    waypoints.sort(key=lambda w: w.timestamp)
    
    return waypoints


def parse_timestamp(time_str: str) -> Optional[datetime]:
    """Parse various timestamp formats from GPX files."""
    if not time_str:
        return None
    
    formats = [
        '%Y-%m-%dT%H:%M:%S.%fZ',
        '%Y-%m-%dT%H:%M:%SZ',
        '%Y-%m-%dT%H:%M:%S.%f%z',
        '%Y-%m-%dT%H:%M:%S%z',
        '%Y-%m-%dT%H:%M:%S.%f',
        '%Y-%m-%dT%H:%M:%S',
    ]
    
    for fmt in formats:
        try:
            return datetime.strptime(time_str, fmt).replace(tzinfo=timezone.utc)
        except ValueError:
            continue
    
    # Try ISO format as fallback
    try:
        return datetime.fromisoformat(time_str.replace('Z', '+00:00'))
    except ValueError:
        pass
    
    return None


def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """
    Calculate the great-circle distance between two points on Earth.
    Returns distance in meters.
    """
    R = 6371000  # Earth's radius in meters
    
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    
    a = math.sin(delta_phi / 2) ** 2 + \
        math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    
    return R * c


def analyze_intervals(
    waypoints: List[Waypoint],
    anomaly_threshold_seconds: float = 120,
    min_interval_seconds: float = 0.5
) -> GpxAnalysisResult:
    """
    Analyze intervals between waypoints and detect anomalies.
    
    Args:
        waypoints: List of parsed waypoints
        anomaly_threshold_seconds: Intervals longer than this are considered anomalies
        min_interval_seconds: Minimum expected interval (for detecting too-frequent points)
    """
    if len(waypoints) < 2:
        return GpxAnalysisResult(
            filename="",
            total_waypoints=len(waypoints),
            total_duration_seconds=0,
            intervals=[],
            anomalies=[],
            statistics={
                'mean_interval': 0,
                'median_interval': 0,
                'stdev_interval': 0,
                'min_interval': 0,
                'max_interval': 0,
                'total_distance_m': 0,
                'anomaly_count': 0,
                'anomaly_percentage': 0
            }
        )
    
    intervals = []
    anomalies = []
    
    for i in range(len(waypoints) - 1):
        wp1 = waypoints[i]
        wp2 = waypoints[i + 1]
        
        # Calculate time interval
        interval_seconds = (wp2.timestamp - wp1.timestamp).total_seconds()
        
        # Calculate distance
        distance = haversine_distance(wp1.lat, wp1.lon, wp2.lat, wp2.lon)
        
        # Calculate speed (m/s)
        speed_ms = distance / interval_seconds if interval_seconds > 0 else 0
        
        interval = IntervalAnalysis(
            from_waypoint=wp1,
            to_waypoint=wp2,
            interval_seconds=interval_seconds,
            distance_meters=distance,
            speed_ms=speed_ms
        )
        
        intervals.append(interval)
        
        # Detect anomalies
        if interval_seconds > anomaly_threshold_seconds:
            interval.is_anomaly = True
            interval.anomaly_type = f"Large gap ({interval_seconds:.1f}s > {anomaly_threshold_seconds}s threshold)"
            anomalies.append(interval)
        elif interval_seconds < min_interval_seconds and interval_seconds > 0:
            interval.is_anomaly = True
            interval.anomaly_type = f"Very short interval ({interval_seconds:.2f}s < {min_interval_seconds}s minimum)"
            anomalies.append(interval)
    
    # Calculate statistics
    interval_times = [i.interval_seconds for i in intervals if i.interval_seconds > 0]
    
    stats_dict = {
        'mean_interval': stats_module.mean(interval_times) if interval_times else 0,
        'median_interval': stats_module.median(interval_times) if interval_times else 0,
        'stdev_interval': stats_module.stdev(interval_times) if len(interval_times) > 1 else 0,
        'min_interval': min(interval_times) if interval_times else 0,
        'max_interval': max(interval_times) if interval_times else 0,
        'total_distance_m': sum(i.distance_meters for i in intervals),
        'anomaly_count': len(anomalies),
        'anomaly_percentage': (len(anomalies) / len(intervals) * 100) if intervals else 0
    }
    
    # Detect inconsistent patterns (intervals that deviate significantly from mean)
    if len(interval_times) > 2 and stats_dict['stdev_interval'] > 0:
        for interval in intervals:
            if not interval.is_anomaly and interval.interval_seconds > 0:
                z_score = abs(interval.interval_seconds - stats_dict['mean_interval']) / stats_dict['stdev_interval']
                if z_score > 3:  # More than 3 standard deviations
                    interval.is_anomaly = True
                    interval.anomaly_type = f"Inconsistent interval (z-score: {z_score:.2f})"
                    anomalies.append(interval)
    
    total_duration = (waypoints[-1].timestamp - waypoints[0].timestamp).total_seconds()
    
    return GpxAnalysisResult(
        filename="",
        total_waypoints=len(waypoints),
        total_duration_seconds=total_duration,
        intervals=intervals,
        anomalies=anomalies,
        statistics=stats_dict
    )


def format_duration(seconds: float) -> str:
    """Format seconds as human-readable duration."""
    hours = int(seconds // 3600)
    minutes = int((seconds % 3600) // 60)
    secs = int(seconds % 60)
    
    if hours > 0:
        return f"{hours}h {minutes}m {secs}s"
    elif minutes > 0:
        return f"{minutes}m {secs}s"
    else:
        return f"{secs}s"


def print_analysis(result: GpxAnalysisResult, verbose: bool = False):
    """Print formatted analysis results."""
    print(f"\n{'='*70}")
    print(f"GPX Analysis: {result.filename}")
    print(f"{'='*70}")
    
    print(f"\n[STATISTICS] General Statistics:")
    print(f"  Total waypoints: {result.total_waypoints}")
    print(f"  Total duration: {format_duration(result.total_duration_seconds)}")
    total_distance = result.statistics.get('total_distance_m', 0)
    print(f"  Total distance: {total_distance / 1000:.2f} km")
    
    print(f"\n[INTERVALS] Interval Statistics:")
    print(f"  Mean interval: {result.statistics['mean_interval']:.2f}s")
    print(f"  Median interval: {result.statistics['median_interval']:.2f}s")
    print(f"  Standard deviation: {result.statistics['stdev_interval']:.2f}s")
    print(f"  Min interval: {result.statistics['min_interval']:.2f}s")
    print(f"  Max interval: {format_duration(result.statistics['max_interval'])}")
    
    print(f"\n[ANOMALIES] Anomalies:")
    print(f"  Total anomalies: {result.statistics['anomaly_count']}")
    print(f"  Anomaly percentage: {result.statistics['anomaly_percentage']:.1f}%")
    
    if result.anomalies:
        print(f"\n  Detailed anomalies:")
        for i, anomaly in enumerate(result.anomalies[:10], 1):  # Show first 10
            print(f"    {i}. {anomaly.anomaly_type}")
            print(f"       From: {anomaly.from_waypoint.timestamp.strftime('%H:%M:%S')}")
            print(f"       To:   {anomaly.to_waypoint.timestamp.strftime('%H:%M:%S')}")
            print(f"       Distance: {anomaly.distance_meters:.1f}m")
            if anomaly.speed_ms > 0:
                print(f"       Speed: {anomaly.speed_ms * 3.6:.1f} km/h")
            print()
        
        if len(result.anomalies) > 10:
            print(f"    ... and {len(result.anomalies) - 10} more anomalies")
    
    if verbose and result.intervals:
        print(f"\n[DETAILS] All Intervals:")
        for i, interval in enumerate(result.intervals):
            marker = "[!]" if interval.is_anomaly else "[ ]"
            print(f"{marker} [{i+1}] {interval.interval_seconds:.2f}s | "
                  f"{interval.distance_meters:.1f}m | "
                  f"{interval.speed_ms * 3.6:.1f} km/h")


def analyze_file(filepath: str, args) -> GpxAnalysisResult:
    """Analyze a single GPX file."""
    waypoints = parse_gpx_file(filepath)
    result = analyze_intervals(
        waypoints,
        anomaly_threshold_seconds=args.threshold,
        min_interval_seconds=args.min_interval
    )
    result.filename = filepath
    return result


def main():
    parser = argparse.ArgumentParser(
        description='Analyze GPX files for waypoint interval anomalies',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python gpx_analyzer.py track.gpx
  python gpx_analyzer.py ../examples/ --recursive
  python gpx_analyzer.py *.gpx --threshold 60 --verbose
        """
    )
    
    parser.add_argument('path', help='Path to GPX file or directory')
    parser.add_argument('-r', '--recursive', action='store_true',
                        help='Recursively search for GPX files in directories')
    parser.add_argument('-t', '--threshold', type=float, default=120,
                        help='Anomaly threshold in seconds (default: 120)')
    parser.add_argument('--min-interval', type=float, default=0.5,
                        help='Minimum expected interval in seconds (default: 0.5)')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='Show detailed interval listing')
    parser.add_argument('-o', '--output', type=str,
                        help='Save report to file')
    
    args = parser.parse_args()
    
    # Collect files to analyze
    files_to_analyze = []
    path = Path(args.path)
    
    if path.is_file():
        if path.suffix.lower() == '.gpx':
            files_to_analyze.append(str(path))
        else:
            print(f"Error: {path} is not a GPX file", file=sys.stderr)
            sys.exit(1)
    elif path.is_dir():
        pattern = '**/*.gpx' if args.recursive else '*.gpx'
        files_to_analyze = [str(f) for f in path.glob(pattern)]
        
        if not files_to_analyze:
            print(f"No GPX files found in {path}", file=sys.stderr)
            sys.exit(1)
    else:
        # Handle glob patterns
        import glob as glob_module
        files_to_analyze = glob_module.glob(args.path)
        if not files_to_analyze:
            print(f"No files match pattern: {args.path}", file=sys.stderr)
            sys.exit(1)
    
    # Analyze all files
    results = []
    for filepath in files_to_analyze:
        try:
            result = analyze_file(filepath, args)
            results.append(result)
            print_analysis(result, args.verbose)
        except Exception as e:
            print(f"Error analyzing {filepath}: {e}", file=sys.stderr)
    
    # Summary
    if len(results) > 1:
        print(f"\n{'='*70}")
        print(f"SUMMARY: Analyzed {len(results)} files")
        print(f"{'='*70}")
        total_anomalies = sum(r.statistics['anomaly_count'] for r in results)
        total_waypoints = sum(r.total_waypoints for r in results)
        print(f"  Total waypoints: {total_waypoints}")
        print(f"  Total anomalies: {total_anomalies}")
        print(f"  Files with anomalies: {sum(1 for r in results if r.anomalies)}/{len(results)}")
    
    # Save to file if requested
    if args.output and results:
        with open(args.output, 'w') as f:
            f.write("# GPX Interval Analysis Report\n\n")
            for result in results:
                f.write(f"## {result.filename}\n\n")
                f.write(f"- Total waypoints: {result.total_waypoints}\n")
                f.write(f"- Total duration: {format_duration(result.total_duration_seconds)}\n")
                f.write(f"- Mean interval: {result.statistics['mean_interval']:.2f}s\n")
                f.write(f"- Anomalies: {result.statistics['anomaly_count']}\n\n")
                
                if result.anomalies:
                    f.write("### Anomalies:\n\n")
                    for anomaly in result.anomalies:
                        f.write(f"- {anomaly.anomaly_type}\n")
                        f.write(f"  Time: {anomaly.from_waypoint.timestamp.isoformat()} -> "
                               f"{anomaly.to_waypoint.timestamp.isoformat()}\n")
                        f.write(f"  Interval: {anomaly.interval_seconds:.2f}s\n\n")
        
        print(f"\nReport saved to: {args.output}")


if __name__ == '__main__':
    main()

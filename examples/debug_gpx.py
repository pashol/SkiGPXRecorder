#!/usr/bin/env python3
"""
Debug script to analyze GPX parsing issues between snowtrack and Day_5 files.
"""
import xml.etree.ElementTree as ET
import sys

def parse_gpx_debug(filepath):
    """Parse a GPX file and debug the process."""
    print(f"\n{'='*60}")
    print(f"DEBUGGING: {filepath}")
    print(f"{'='*60}")
    
    # Read the file content first
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        print(f"\n1. File read successfully")
        print(f"   - File size: {len(content)} bytes")
        print(f"   - Line count: {content.count(chr(10)) + 1}")
        print(f"   - First 200 chars: {content[:200]}")
    except Exception as e:
        print(f"ERROR reading file: {e}")
        return
    
    # Try to parse the XML
    try:
        root = ET.fromstring(content)
        print(f"\n2. XML parsed successfully")
        print(f"   - Root tag: {root.tag}")
        print(f"   - Root attributes: {dict(root.attrib)}")
    except Exception as e:
        print(f"ERROR parsing XML: {e}")
        return
    
    # Check namespaces
    print(f"\n3. Namespace Analysis:")
    # Register namespaces
    namespaces = {
        'gpx': 'http://www.topografix.com/GPX/1/1',
        'gpx11': 'http://www.topografix.com/GPX/1/1/',
        'xsi': 'http://www.w3.org/2001/XMLSchema-instance'
    }
    
    # Try different namespace variations
    ns_variations = [
        ('http://www.topografix.com/GPX/1/1', 'GPX/1/1 (no trailing slash)'),
        ('http://www.topografix.com/GPX/1/1/', 'GPX/1/1/ (with trailing slash)'),
    ]
    
    for ns_url, description in ns_variations:
        print(f"\n   Testing with {description}:")
        ns = {'gpx': ns_url}
        
        # Try to find trk elements
        trk_elements = root.findall('.//gpx:trk', ns)
        print(f"   - Found {len(trk_elements)} trk elements")
        
        # Try to find trkpt elements
        trkpt_elements = root.findall('.//gpx:trkpt', ns)
        print(f"   - Found {len(trkpt_elements)} trkpt elements")
        
        if trkpt_elements:
            # Check first trkpt
            first_trkpt = trkpt_elements[0]
            print(f"   - First trkpt attrib: {dict(first_trkpt.attrib)}")
            
            # Try to find time elements
            time_elements = first_trkpt.findall('.//gpx:time', ns)
            print(f"   - Found {len(time_elements)} time elements in first trkpt")
    
    # Check what's actually in the root
    print(f"\n4. Direct child elements of root:")
    for child in root:
        print(f"   - {child.tag}")
    
    # Look at trk elements directly
    print(f"\n5. Looking for trk elements (no namespace):")
    trk_no_ns = root.findall('.//trk')
    print(f"   - Found {len(trk_no_ns)} trk elements without namespace")
    
    print(f"\n6. Looking for trkpt elements (no namespace):")
    trkpt_no_ns = root.findall('.//trkpt')
    print(f"   - Found {len(trkpt_no_ns)} trkpt elements without namespace")
    
    # Try extracting namespace from root tag
    print(f"\n7. Extracting namespace from root tag:")
    if '}' in root.tag:
        ns_from_tag = root.tag.split('}')[0].strip('{')
        print(f"   - Namespace from root tag: {ns_from_tag}")
    else:
        print(f"   - No namespace prefix in root tag")
        # The default namespace is in xmlns attribute
        print(f"   - Checking for xmlns attribute...")
    
    # Get the actual namespace map from the element
    print(f"\n8. Element namespace inspection:")
    print(f"   - Root tag: {repr(root.tag)}")
    
    # Try parsing with the exact namespace from the root element
    if root.tag.startswith('{'):
        actual_ns = root.tag.split('}')[0][1:]
        print(f"   - Actual namespace URI: {actual_ns}")
        
        ns_exact = {'gpx': actual_ns}
        trkpt_exact = root.findall('.//gpx:trkpt', ns_exact)
        print(f"   - Found {len(trkpt_exact)} trkpt elements with exact namespace")
    
    print(f"\n{'='*60}")
    print(f"END DEBUG: {filepath}")
    print(f"{'='*60}")

if __name__ == '__main__':
    files = [
        r'C:\Users\pasca\Coding\SkiGPXRecorder\examples\snowtrack-track-2026-01-01T12_30_15.350Z.gpx',
        r'C:\Users\pasca\Coding\SkiGPXRecorder\examples\Day_5_2024-2025.gpx'
    ]
    
    for filepath in files:
        parse_gpx_debug(filepath)
        print("\n")

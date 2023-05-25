package com.hcmut.test;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public class NavTest {
    public static final List<Location> locations;
    static {
        locations = new ArrayList<>() {{
            /*
                10.72898, 106.72696
                10.729, 106.72694
                10.72875, 106.72654
                10.72858, 106.72625
                10.72846, 106.72606
                10.72837, 106.72584
                10.72836, 106.7259
                10.72834, 106.72584
                10.72814, 106.72554
                10.72766, 106.72477
                10.72729, 106.72413
                10.72702, 106.72366
                10.72676, 106.72317
                10.72711, 106.72297
                10.72724, 106.72289
                10.72757, 106.72266
                10.72825, 106.72223
                10.729, 106.72179
                10.72924, 106.7216
                10.7297, 106.72136
                10.73006, 106.72114
            * */
            add(new Location("") {{
                setLatitude(10.72898);
                setLongitude(106.72696);
            }});
            add(new Location("") {{
                setLatitude(10.729);
                setLongitude(106.72694);
            }});
            add(new Location("") {{
                setLatitude(10.72875);
                setLongitude(106.72654);
            }});
            add(new Location("") {{
                setLatitude(10.72858);
                setLongitude(106.72625);
            }});
            add(new Location("") {{
                setLatitude(10.72846);
                setLongitude(106.72606);
            }});
            add(new Location("") {{
                setLatitude(10.72837);
                setLongitude(106.72584);
            }});
            add(new Location("") {{
                setLatitude(10.72836);
                setLongitude(106.7259);
            }});
            add(new Location("") {{
                setLatitude(10.72834);
                setLongitude(106.72584);
            }});
            add(new Location("") {{
                setLatitude(10.72814);
                setLongitude(106.72554);
            }});
            add(new Location("") {{
                setLatitude(10.72766);
                setLongitude(106.72477);
            }});
            add(new Location("") {{
                setLatitude(10.72729);
                setLongitude(106.72413);
            }});
            add(new Location("") {{
                setLatitude(10.72702);
                setLongitude(106.72366);
            }});
            add(new Location("") {{
                setLatitude(10.72676);
                setLongitude(106.72317);
            }});
            add(new Location("") {{
                setLatitude(10.72711);
                setLongitude(106.72297);
            }});
            add(new Location("") {{
                setLatitude(10.72724);
                setLongitude(106.72289);
            }});
            add(new Location("") {{
                setLatitude(10.72757);
                setLongitude(106.72266);
            }});
            add(new Location("") {{
                setLatitude(10.72825);
                setLongitude(106.72223);
            }});
            add(new Location("") {{
                setLatitude(10.729);
                setLongitude(106.72179);
            }});
            add(new Location("") {{
                setLatitude(10.72924);
                setLongitude(106.7216);
            }});
            add(new Location("") {{
                setLatitude(10.7297);
                setLongitude(106.72136);
            }});
            add(new Location("") {{
                setLatitude(10.73006);
                setLongitude(106.72114);
            }});
        }};

        // generate first bearing is bearing to the next point, then others are bearing from previous point to current point
        float bearing = locations.get(0).bearingTo(locations.get(1));
        locations.get(0).setBearing(bearing);
        for (int i = 1; i < locations.size(); i++) {
            bearing = locations.get(i - 1).bearingTo(locations.get(i));
            locations.get(i).setBearing(bearing);
        }
    }
}

Here is a functional, feature-focused roadmap for building your offline-first restaurant finder, structured around core app mechanics and UI flow rather than build configurations.
Phase 1: Local Data Layer & Asset Ingestion

    Room Schema & Asset Binding:

        Define the Restaurant entity matching your pre-built SQLite table (id, name, amenity, cuisine, city, street, housenumber, latitude, longitude, phone, website).

        Configure Room to hydrate the database on first run from your bundled .db asset.

    DAO Query Design:

        Coordinate bounding-box queries (latitude BETWEEN ... AND longitude BETWEEN ...) to limit the active candidate pool before calculating distances.

        Category / Amenity filtering queries (e.g., dynamic WHERE amenity IN (:includedTypes)).

        Text search query against name, cuisine, and city.

Phase 2: Location Engine & Geographic Calculation

    Device Location Provider:

        Integrate location permissions flow (Coarse vs. Fine Location).

        Fetch current device coordinates (single-fix/last-known location on launch, rather than high-frequency continuous tracking).

    Manual Location / Area Override:

        Create an override state: allow picking a preset municipality (e.g., Toledo, Perrysburg, Monroe, Adrian) or setting a custom anchor coordinate.

        Implement an active location toggle: Use GPS vs. Use Selected Area.

    Client-Side Distance & Sorting:

        Calculate straight-line distances (Haversine formula or Location.distanceBetween) from the active anchor point to candidate venues.

        Maintain dynamic distance labels in the UI (e.g., 1.2 mi, 450 ft) and support ascending distance sort.

Phase 3: Core UI Architecture & Navigation

A clean, map-free interface can be built around two primary screens:
1. Main Directory Screen (Search & Browse)

    Top App Bar / Anchor Status:

        Shows the current active center point (e.g., "Near Current Location" or "Near Adrian, MI") with an edit/override action.

        Search bar for quick text filtering by name or cuisine tag.

    Filter Strip / Chips:

        Quick-toggle chips for venue types: Sit-Down / Full Service, Fast Food (easily toggled off), Cafes, Bars / Pubs.

        Distance radius selector (e.g., 2 mi, 5 mi, 10 mi, 25 mi).

    Results Feed (Scrollable List):

        Clean cards displaying:

            Venue name and primary cuisine/category tag.

            Relative distance from active anchor.

            City / Street address line.

        Visual badge indicating if external details (website, phone) are available.

2. Restaurant Detail Screen (or Bottom Sheet)

    Shows complete address, cuisine tags, and opening hours (if present).

    Action buttons for external launches.

Phase 4: System Integrations & External Launchers

    External Map Navigation Intent:

        When a restaurant is clicked or a "Navigate" button is tapped, dispatch an implicit geo: intent or a Universal Navigation URI:

        geo:0,0?q=latitude,longitude(Restaurant+Name)

        This hands off navigation directly to the user's preferred default map app (Google Maps, OsmAnd, Organic Maps, etc.).

    External Web Browser Launch:

        For venues with a website tag, launch an implicit ACTION_VIEW intent with the parsed URI to open the link in the device's default web browser.

    Dialer Intent (Optional convenience):

        One-tap ACTION_DIAL intent to open phone numbers directly in the system dialer.

Phase 5: Filter State & Preferences Persistence

    Preference Storage (DataStore / SharedPreferences):

        Persist the user's category inclusions/exclusions across sessions (e.g., keeping "Fast Food" permanently excluded by default).

        Remember the last selected radius and sort preference.

        Remember the last chosen manual override location if GPS is disabled.

Phase 6: Sync & Maintenance Pipeline

    Background Sync / Refresh Engine:

        Periodic background task (or manual "Check for Updates" button in settings).

        Pulls fresh Overpass bounding-box JSON over the network.

        Executes batch @Upsert operations into the existing Room database so new or corrected locations appear without blowing away user settings.

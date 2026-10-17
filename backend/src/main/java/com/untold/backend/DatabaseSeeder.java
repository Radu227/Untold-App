package com.untold.backend;

import com.untold.backend.model.User;
import com.untold.backend.model.UserType;
import com.untold.backend.model.VenueMap;
import com.untold.backend.model.MapPin;
import com.untold.backend.repository.UserRepository;
import com.untold.backend.repository.VenueMapRepository;
import com.untold.backend.repository.MapPinRepository;
import com.untold.backend.service.AuthService;
import com.untold.backend.service.ArtistService;
import com.untold.backend.model.TicketTier;
import com.untold.backend.model.TicketCategory;
import com.untold.backend.model.TicketType;
import com.untold.backend.model.Ticket;
import com.untold.backend.repository.TicketTierRepository;
import com.untold.backend.repository.TicketRepository;
import com.untold.backend.service.TicketService;
import com.untold.backend.model.Artist;
import com.untold.backend.model.Performance;
import com.untold.backend.model.PerformanceStatus;
import com.untold.backend.repository.ArtistRepository;
import com.untold.backend.repository.PerformanceRepository;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.untold.backend.model.Person;
import com.untold.backend.model.AppOrder;
import com.untold.backend.model.OrderType;
import com.untold.backend.model.DocumentType;
import com.untold.backend.repository.PersonRepository;
import com.untold.backend.repository.OrderRepository;
import com.untold.backend.model.StaffMember;
import com.untold.backend.repository.StaffMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final VenueMapRepository venueMapRepository;
    private final MapPinRepository mapPinRepository;
    private final ArtistService artistService;
    private final TicketTierRepository ticketTierRepository;
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final ArtistRepository artistRepository;
    private final PerformanceRepository performanceRepository;
    private final PersonRepository personRepository;
    private final OrderRepository orderRepository;
    private final StaffMemberRepository staffMemberRepository;

    @Value("${flash.sale.total.tickets}")
    private int flashSaleTotalTickets;

    public DatabaseSeeder(AuthService authService, 
                          UserRepository userRepository, 
                          VenueMapRepository venueMapRepository, 
                          MapPinRepository mapPinRepository,
                          ArtistService artistService,
                          TicketTierRepository ticketTierRepository,
                          TicketRepository ticketRepository,
                          TicketService ticketService,
                          ArtistRepository artistRepository,
                          PerformanceRepository performanceRepository,
                          PersonRepository personRepository,
                          OrderRepository orderRepository,
                          StaffMemberRepository staffMemberRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.venueMapRepository = venueMapRepository;
        this.mapPinRepository = mapPinRepository;
        this.artistService = artistService;
        this.ticketTierRepository = ticketTierRepository;
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
        this.artistRepository = artistRepository;
        this.performanceRepository = performanceRepository;
        this.personRepository = personRepository;
        this.orderRepository = orderRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    @Override
    public void run(String... args) {
        // Seed multiple test attendee users
        seedAttendee("john@gmail.com", "john", "John Doe");
        seedAttendee("maria@gmail.com", "maria", "Maria Popescu");
        seedAttendee("andrei@gmail.com", "andrei", "Andrei Ionescu");
        seedAttendee("elena@gmail.com", "elena", "Elena Radu");
        seedAttendee("vlad@gmail.com", "vlad", "Vlad Călin");

        String email2 = "bob@gmail.com";
        if (userRepository.findByEmail(email2).isEmpty()) {
            User user = new User();
            user.setUsername("Bob");
            user.setEmail(email2);
            user.setPassword("bob");
            user.setUserType(UserType.ATTENDEE);

            authService.register(user);
            System.out.println("Seeded test user: " + email2);
        } else {
            System.out.println("Test user already exists: " + email2);
        }

        String adminEmail = "admin@untold.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setUsername("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword("admin");
            admin.setUserType(UserType.ADMIN);

            authService.register(admin);
            System.out.println("Seeded admin user: " + adminEmail);
        } else {
            System.out.println("Admin user already exists: " + adminEmail);
        }

        // Seed 3 new default admin users as requested
        String[] adminEmails = {"admin1@untold.com", "admin2@untold.com", "admin3@untold.com"};
        for (int i = 0; i < adminEmails.length; i++) {
            String emailStr = adminEmails[i];
            if (userRepository.findByEmail(emailStr).isEmpty()) {
                User adminUser = new User();
                adminUser.setUsername("Admin " + (i + 1));
                adminUser.setEmail(emailStr);
                adminUser.setPassword("admin123");
                adminUser.setUserType(UserType.ADMIN);

                authService.register(adminUser);
                System.out.println("Seeded default Admin: " + emailStr);
            } else {
                System.out.println("Default Admin already exists: " + emailStr);
            }
        }

        // Seed Venue Map and Pins
        VenueMap savedMap;
        if (venueMapRepository.count() == 0) {
            VenueMap map = new VenueMap();
            map.setImageUrl("/uploads/96a24512-9428-452d-8d45-3f9d7bcff88e.png");
            map.setActive(true);
            savedMap = venueMapRepository.save(map);
            System.out.println("Seeded default Untold Venue Map");
        } else {
            savedMap = venueMapRepository.findAll().get(0);
        }

        // Seed/Update Pins unconditionally (will use our new name-based update-or-insert logic)
        seedPin(savedMap, "STAGE", "Main Stage", "-", 72.38, 27.83);
        seedPin(savedMap, "STAGE", "Galaxy Stage", "-", 79.44, 12.94);
        seedPin(savedMap, "STAGE", "Alchemy Stage", "-", 85.67, 21.77);
        seedPin(savedMap, "STAGE", "Time Stage", "-", 55.98, 20.86);
        seedPin(savedMap, "STAGE", "Daydreaming Stage", "-", 65.16, 41.45);
        seedPin(savedMap, "STAGE", "Soul Circle Stage", "-", 45.8, 46.21);
        seedPin(savedMap, "STAGE", "Retro Stage", "-", 45.31, 66.34);
        seedPin(savedMap, "FIRST_AID", "First Aid 1", "-", 71.73, 19.05);
        seedPin(savedMap, "FIRST_AID", "First Aid 2", "-", 50.56, 27.65);
        seedPin(savedMap, "FIRST_AID", "First Aid 3", "-", 79.76, 30.82);
        seedPin(savedMap, "EXIT", "Exit 1", "-", 47.61, 21.32);
        seedPin(savedMap, "EXIT", "Exit 3", "-", 33.66, 75.17);
        seedPin(savedMap, "ENTRANCE", "General Access", "-", 26.12, 68.61);
        seedPin(savedMap, "ENTRANCE", "VIP Access", "-", 22.23, 64.28);
        seedPin(savedMap, "TOILET", "Toilets 1", "-", 50.0, 64.03);
        seedPin(savedMap, "STAGE", "Tram Stage", "-", 79.89, 41.82);
        seedPin(savedMap, "EXIT", "Exit 2", "-", 76.44, 48.06);
        seedPin(savedMap, "TOILET", "Toilets 2", "-", 78.11, 34.52);
        seedPin(savedMap, "TOILET", "Toilets 3", "-", 63.5, 24.19);
        seedPin(savedMap, "EXIT", "Exit 4", "-", 71.86, 5.32);
        seedPin(savedMap, "TOILET", "Toilets 4", "-", 75.32, 6.4);
        seedPin(savedMap, "FANTASIA", "Fantasia", "-", 72.64, 37.63);
        seedPin(savedMap, "FOOD_COURT", "Food Truck", "-", 59.48, 17.45);
        seedPin(savedMap, "BAR", "Bar 1", "-", 33.38, 59.17);
        seedPin(savedMap, "BAR", "Bar 2", "-", 43.87, 59.48);
        seedPin(savedMap, "BAR", "Bar 3", "-", 42.53, 70.43);
        seedPin(savedMap, "BAR", "Bar 4", "-", 61.6, 41.63);
        seedPin(savedMap, "BAR", "Bar 5", "-", 52.79, 34.99);
        seedPin(savedMap, "BAR", "Bar 6", "-", 84.13, 18.22);
        seedPin(savedMap, "BAR", "Bar 7", "-", 58.59, 27.29);
        seedPin(savedMap, "BAR", "Bar 8", "-", 76.77, 17.14);
        seedPin(savedMap, "BAR", "Bar 9", "-", 69.07, 23.75);
        seedPin(savedMap, "FOOD_COURT", "Food Truck", "-", 67.07, 35.91);
        seedPin(savedMap, "FOOD_COURT", "Food Truck", "-", 70.52, 41.14);
        seedPin(savedMap, "FOOD_COURT", "Food Truck", "-", 62.94, 13.6);
        seedPin(savedMap, "FOOD_COURT", "Dinning Zone", "-", 66.17, 11.6);
        seedPin(savedMap, "FOOD_COURT", "Food Truck", "-", 55.13, 55.17);
        seedPin(savedMap, "CREDIT_POINT", "Credit Point 1", "-", 31.82, 67.42);
        seedPin(savedMap, "CREDIT_POINT", "Credit Point 2", "-", 40.3, 54.19);
        seedPin(savedMap, "CREDIT_POINT", "Credit Point 3", "-", 50.34, 39.57);
        seedPin(savedMap, "CREDIT_POINT", "Credit Point 4", "-", 62.72, 33.02);
        seedPin(savedMap, "CREDIT_POINT", "Credit Point 5", "-", 65.73, 18.34);
        seedPin(savedMap, "FASHION_CREATOR", "Designer booth", "-", 55.02, 46.8);
        seedPin(savedMap, "FOOD_COURT", "Dinning Zone 2", "-", 58.14, 52.83);
        seedPin(savedMap, "FASHION_CREATOR", "Merchandising", "-", 52.57, 43.02);

        System.out.println("All map pins verified and synchronized.");

        // Seed default artists and tracks if they are missing
        System.out.println("Checking and syncing missing high-profile Untold 2026 artists and their top tracks...");
        String[] artistIds = {
            "430932944",  // Martin Garrix
            "1062085272", // Alan Walker
            "635806094",  // Kygo
            "980795202",  // Marshmello
            "580391756",  // The Chainsmokers
            "570372593",  // Zara Larsson
            "1213405916", // Lewis Capaldi
            "94804",      // Sting
            "271066694",  // Steve Aoki
            "877792512",  // Lost Frequencies
            "211210469",  // Afrojack
            "255303209",  // Flo Rida
            "925734064",  // James Hype
            "268115002",  // Maddix
            "2811492",    // Carl Cox
            "16013761",   // Jamie Jones
            "258951272",  // Joseph Capriati
            "1636676418", // Mau P
            "1332641349", // Sara Landry
            "401736318",  // Maceo Plex
            "1555757371", // Gordo
            "77816993",   // Sebastian Ingrosso
            "331900515",  // R3hab
            "1356734441", // Smiley
            "922547500",  // Swae Lee
            "682631805",  // Tchami
            "335403205",  // Tujamo
            "1449138676", // Meduza
            "200779145",  // Solomun
            "80020595",   // Nic Fanciulli
            "307582974",  // Cristi Cons
            "1386592541", // Celavi
            "468500851",  // Jan Blomqvist
            "29431640",   // Sebastien Leger
            "285797017",  // Mano Le Tough
            "697739898",  // Shimza
            "631440154",  // Denzel Curry
            "288242555",  // Camo & Krooked
            "111544331",  // Andy C
            "301510573",  // Gramatik
            "1161580951", // bbno$
            "1402090482", // Erika Isac
            "188933506",  // Grasu XXL
            "320627998",  // Parazitii
            "118007099"   // Pendulum
        };

        int seededCount = 0;
        for (String artistId : artistIds) {
            if (artistRepository.findBySpotifyId(artistId).isEmpty()) {
                try {
                    artistService.syncArtistFromSpotify(artistId);
                    seededCount++;
                } catch (Exception e) {
                    System.err.println("Warning: Failed to seed artist ID " + artistId + ": " + e.getMessage());
                }
            } else {
                seededCount++;
            }
        }
        System.out.println("Successfully verified/seeded " + seededCount + " / " + artistIds.length + " Untold artists and their top tracks.");

        // Seed default Ticket Tiers from TicketType enum constants!
        System.out.println("Syncing/Seeding dynamic Ticket Tiers from enum constants...");
        for (TicketType type : TicketType.values()) {
            if (ticketTierRepository.findById(type.name()).isEmpty()) {
                long cap = 15000L;
                if (type.getCategory() == TicketCategory.VIP) {
                    cap = 3000L;
                } else if (type.getCategory() == TicketCategory.BACKSTAGE) {
                    cap = 500L;
                }

                Integer qty = type == TicketType.GA_4_DAYS_FLASH_SALE ? flashSaleTotalTickets : null;

                ticketTierRepository.save(TicketTier.builder()
                        .id(type.name())
                        .displayName(type.getDisplayName())
                        .category(type.getCategory())
                        .price(type.getDefaultPrice())
                        .capacity(cap)
                        .quantity(qty)
                        .sold(0L)
                        .active(true)
                        .build());
                System.out.println("Seeded missing Ticket Tier: " + type.name());
            }
        }

        // Recalculate/Sync all ticket tier sold counts based on actual tickets in the database
        System.out.println("Re-syncing and reconciling sold counts for all ticket tiers...");
        for (TicketTier tier : ticketTierRepository.findAll()) {
            long actualSold = ticketRepository.countByTicketType(tier.getId());
            if (tier.getSold() != actualSold) {
                tier.setSold(actualSold);
                ticketTierRepository.save(tier);
                System.out.println("Reconciled sold count for tier " + tier.getId() + " to " + actualSold);
            }
        }

        // Seed default dynamic tickets if none exist
        if (ticketRepository.count() < 100) {
            System.out.println("No substantial ticket data found (< 100). Seeding premium real-time data for 3 Live Analytics Widgets...");

            java.util.List<User> attendees = userRepository.findAll().stream()
                    .filter(u -> u.getUserType() == UserType.ATTENDEE)
                    .collect(java.util.stream.Collectors.toList());

            if (!attendees.isEmpty()) {
                java.util.Random random = new java.util.Random(42); // deterministic seed for stability

                String[] firstNames = {"Alexandru", "Mihai", "Andrei", "David", "Matei", "Gabriel", "Ioana", "Maria", "Elena", "Andreea", "Stefan", "Sebastian", "Laura", "Diana", "Cristian", "Lucas", "Emma", "Chloe", "Zoe", "Sarah", "Thomas", "Maximilian", "Lukas", "Anna", "Clara", "Zsolt", "Bence"};
                String[] lastNames = {"Popescu", "Ionescu", "Radu", "Calin", "Dumitru", "Nica", "Stoica", "Gheorghe", "Mihailescu", "Voinea", "Sandu", "Munteanu", "Dragomir", "Muller", "Schmidt", "Schneider", "Smith", "Johnson", "Williams", "Kovacs", "Nagy", "Szabo"};

                // 15 days of ticket purchasing history (Days ago: 14 down to 0)
                int[] dailyCounts = {22, 27, 25, 34, 31, 41, 39, 45, 52, 48, 58, 67, 81, 95, 62};
                TicketType[] ticketTypes = TicketType.values();

                int totalSeededTickets = 0;
                int totalSeededCheckedIn = 0;

                for (int dayOffset = 14; dayOffset >= 0; dayOffset--) {
                    int countForDay = dailyCounts[14 - dayOffset];
                    for (int i = 0; i < countForDay; i++) {
                        // 1. Create a Person attendee
                        Person person = new Person();
                        person.setFirstName(firstNames[random.nextInt(firstNames.length)]);
                        person.setLastName(lastNames[random.nextInt(lastNames.length)]);

                        // Age range 18 - 42. Average is around 26.
                        int age = 18 + random.nextInt(25);
                        person.setDateOfBirth(LocalDate.now().minusYears(age).minusDays(random.nextInt(365)));
                        person.setGender(random.nextBoolean() ? "Male" : "Female");

                        // Nationality probabilities
                        int nationalityRoll = random.nextInt(100);
                        String country;
                        String city;
                        if (nationalityRoll < 65) {
                            country = "Romania";
                            String[] roCities = {"Cluj-Napoca", "București", "Timișoara", "Iași", "Brașov", "Constanța", "Sibiu"};
                            city = roCities[random.nextInt(roCities.length)];
                        } else if (nationalityRoll < 77) {
                            country = "Germany";
                            String[] deCities = {"Munich", "Berlin", "Frankfurt", "Hamburg", "Cologne"};
                            city = deCities[random.nextInt(deCities.length)];
                        } else if (nationalityRoll < 85) {
                            country = "United Kingdom";
                            String[] ukCities = {"London", "Manchester", "Birmingham", "Edinburgh"};
                            city = ukCities[random.nextInt(ukCities.length)];
                        } else if (nationalityRoll < 92) {
                            country = "France";
                            String[] frCities = {"Paris", "Lyon", "Marseille", "Nice"};
                            city = frCities[random.nextInt(frCities.length)];
                        } else if (nationalityRoll < 97) {
                            country = "Spain";
                            String[] esCities = {"Barcelona", "Madrid", "Valencia", "Seville"};
                            city = esCities[random.nextInt(esCities.length)];
                        } else {
                            country = "Hungary";
                            String[] huCities = {"Budapest", "Debrecen", "Szeged", "Pécs"};
                            city = huCities[random.nextInt(huCities.length)];
                        }
                        person.setCountry(country);
                        person.setCity(city);
                        person.setDocumentType(random.nextBoolean() ? DocumentType.NATIONAL_ID : DocumentType.PASSPORT);
                        person.setDocumentId(String.format("ID%06d", 100000 + random.nextInt(899999)));
                        person.setPhoneNumber("+407" + String.format("%08d", random.nextInt(99999999)));

                        Person savedPerson = personRepository.save(person);

                        // 2. Create the Ticket
                        TicketType type = ticketTypes[random.nextInt(ticketTypes.length)];
                        Ticket ticket = new Ticket();
                        ticket.setTicketType(type.name());
                        ticket.setPrice(type.getDefaultPrice());

                        // Purchase LocalDateTime within the specific day offset
                        LocalDateTime purchaseTime = LocalDateTime.now()
                                .minusDays(dayOffset)
                                .minusHours(1 + random.nextInt(22))
                                .minusMinutes(random.nextInt(60));
                        ticket.setPurchaseDate(purchaseTime);

                        // About 64% of tickets are checked-in to make check-in stats look real
                        boolean checkedIn = random.nextInt(100) < 64;
                        ticket.setCheckInCompleted(checkedIn);
                        ticket.setAttendee(savedPerson);

                        Ticket savedTicket = ticketRepository.save(ticket);

                        // 3. Create the AppOrder
                        User randomUser = attendees.get(random.nextInt(attendees.size()));
                        AppOrder order = new AppOrder();
                        order.setUser(randomUser);
                        order.setOrderType(OrderType.TICKET_PURCHASE);
                        order.setTicket(savedTicket);
                        order.setAmountPaid(type.getDefaultPrice());
                        order.setPurchaseDate(purchaseTime);
                        orderRepository.save(order);

                        savedTicket.setAppOrders(java.util.List.of(order));
                        ticketRepository.save(savedTicket);

                        totalSeededTickets++;
                        if (checkedIn) {
                            totalSeededCheckedIn++;
                        }
                    }
                }
                System.out.println("Seeded " + totalSeededTickets + " tickets (with " + totalSeededCheckedIn + " checked in) across the last 15 days successfully.");
            } else {
                System.err.println("Warning: Cannot seed ticket history because no attendee users are registered.");
            }
        }

        // Seed specific testing users with predictable checked-in tickets
        seedSpecificTestingUser("ga-only@gmail.com", "pass", "GA Only User", "GA_4_DAYS_PHASE_5");
        seedSpecificTestingUser("vip-only@gmail.com", "pass", "VIP Only User", "VIP_MAINSTAGE_4_DAYS_PHASE_3");

        // Seed official UNTOLD 2026 performance schedule
        System.out.println("Clearing and re-seeding official UNTOLD 2026 performance schedule...");
        performanceRepository.deleteAll();
        seedPerformancesSchedule();
    }

    private void seedAttendee(String email, String password, String name) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setUsername(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setUserType(UserType.ATTENDEE);

            authService.register(user);
            System.out.println("Seeded test attendee: " + email);
        } else {
            System.out.println("Test attendee already exists: " + email);
        }
    }

    private void seedSpecificTestingUser(String email, String password, String name, String ticketTypeStr) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setUsername(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setUserType(UserType.ATTENDEE);

            User savedUser = authService.register(user);
            System.out.println("Seeded specific test user: " + email);

            // Create 1 person as the attendee
            Person person = new Person();
            person.setFirstName(name.split(" ")[0]);
            person.setLastName("Tester");
            person.setDateOfBirth(LocalDate.now().minusYears(25));
            person.setGender("Male");
            person.setCountry("Romania");
            person.setCity("Cluj-Napoca");
            person.setDocumentType(DocumentType.NATIONAL_ID);
            person.setDocumentId("ID999999");
            person.setPhoneNumber("+40799999999");
            Person savedPerson = personRepository.save(person);

            // Create 1 ticket matching the type
            Ticket ticket = new Ticket();
            ticket.setTicketType(ticketTypeStr);
            ticket.setPrice(100.0);
            ticket.setPurchaseDate(LocalDateTime.now());
            ticket.setCheckInCompleted(true); // Always checked-in!
            ticket.setAttendee(savedPerson);
            Ticket savedTicket = ticketRepository.save(ticket);

            // Create 1 order
            AppOrder order = new AppOrder();
            order.setUser(savedUser);
            order.setOrderType(OrderType.TICKET_PURCHASE);
            order.setTicket(savedTicket);
            order.setAmountPaid(100.0);
            order.setPurchaseDate(LocalDateTime.now());
            orderRepository.save(order);

            savedTicket.setAppOrders(java.util.List.of(order));
            ticketRepository.save(savedTicket);
            System.out.println("Seeded checked-in " + ticketTypeStr + " ticket for user: " + email);
        } else {
            System.out.println("Specific test user already exists: " + email);
        }
    }

    private void seedPin(VenueMap map, String type, String name, String description, double x, double y) {
        MapPin pin = mapPinRepository.findAll().stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);

        if (pin == null) {
            pin = new MapPin();
            pin.setVenueMap(map);
            pin.setName(name);
            System.out.println("Seeding new Map Pin: " + name);
        } else {
            System.out.println("Updating existing Map Pin: " + name);
        }
        pin.setType(type);
        pin.setDescription(description);
        pin.setXPos(x);
        pin.setYPos(y);
        mapPinRepository.save(pin);
    }

    private void seedPerformancesSchedule() {
        System.out.println("Seeding official UNTOLD 2026 performance schedule...");

        // Day 1: Thursday, Aug 6, 2026 - Main Stage
        seedPerformance("1356734441", "Main Stage", "2026-08-06T19:30:00+03:00", "2026-08-06T20:45:00+03:00"); // Smiley
        seedPerformance("925734064", "Main Stage", "2026-08-06T21:00:00+03:00", "2026-08-06T22:30:00+03:00");  // James Hype
        seedPerformance("580391756", "Main Stage", "2026-08-06T22:45:00+03:00", "2026-08-07T00:15:00+03:00");  // The Chainsmokers
        seedPerformance("94804", "Main Stage", "2026-08-07T00:30:00+03:00", "2026-08-07T02:00:00+03:00");       // Sting
        seedPerformance("268115002", "Main Stage", "2026-08-07T02:15:00+03:00", "2026-08-07T03:45:00+03:00");  // Maddix

        // Day 1: Thursday, Aug 6, 2026 - Galaxy Stage
        seedPerformance("80020595", "Galaxy Stage", "2026-08-06T22:00:00+03:00", "2026-08-07T00:00:00+03:00"); // Nic Fanciulli
        seedPerformance("2811492", "Galaxy Stage", "2026-08-07T00:00:00+03:00", "2026-08-07T03:00:00+03:00");   // Carl Cox

        // Day 1: Thursday, Aug 6, 2026 - Alchemy Stage
        seedPerformance("188933506", "Alchemy Stage", "2026-08-06T20:00:00+03:00", "2026-08-06T21:30:00+03:00"); // Grasu XXL
        seedPerformance("301510573", "Alchemy Stage", "2026-08-06T21:45:00+03:00", "2026-08-06T23:15:00+03:00"); // Gramatik


        // Day 2: Friday, Aug 7, 2026 - Main Stage
        seedPerformance("570372593", "Main Stage", "2026-08-07T19:30:00+03:00", "2026-08-07T20:45:00+03:00"); // Zara Larsson
        seedPerformance("922547500", "Main Stage", "2026-08-07T21:00:00+03:00", "2026-08-07T22:15:00+03:00");  // Swae Lee
        seedPerformance("682631805", "Main Stage", "2026-08-07T22:30:00+03:00", "2026-08-07T23:45:00+03:00");  // Tchami
        seedPerformance("635806094", "Main Stage", "2026-08-08T00:00:00+03:00", "2026-08-08T01:30:00+03:00");  // Kygo
        seedPerformance("980795202", "Main Stage", "2026-08-08T01:45:00+03:00", "2026-08-08T03:15:00+03:00");  // Marshmello
        seedPerformance("77816993", "Main Stage", "2026-08-08T03:30:00+03:00", "2026-08-08T05:00:00+03:00");   // Sebastian Ingrosso

        // Day 2: Friday, Aug 7, 2026 - Galaxy Stage
        seedPerformance("307582974", "Galaxy Stage", "2026-08-07T21:00:00+03:00", "2026-08-07T23:00:00+03:00"); // Cristi Cons
        seedPerformance("401736318", "Galaxy Stage", "2026-08-07T23:00:00+03:00", "2026-08-08T01:00:00+03:00"); // Maceo Plex
        seedPerformance("1555757371", "Galaxy Stage", "2026-08-08T01:00:00+03:00", "2026-08-08T03:00:00+03:00"); // Gordo

        // Day 2: Friday, Aug 7, 2026 - Daydreaming Stage
        seedPerformance("285797017", "Daydreaming Stage", "2026-08-07T20:00:00+03:00", "2026-08-07T22:00:00+03:00"); // Mano Le Tough
        seedPerformance("29431640", "Daydreaming Stage", "2026-08-07T22:00:00+03:00", "2026-08-08T00:00:00+03:00");  // Sébastien Léger

        // Day 2: Friday, Aug 7, 2026 - Alchemy Stage
        seedPerformance("1402090482", "Alchemy Stage", "2026-08-07T18:30:00+03:00", "2026-08-07T19:45:00+03:00"); // Erika Isac
        seedPerformance("1161580951", "Alchemy Stage", "2026-08-07T20:00:00+03:00", "2026-08-07T21:15:00+03:00"); // bbno$
        seedPerformance("320627998", "Alchemy Stage", "2026-08-07T21:30:00+03:00", "2026-08-07T23:00:00+03:00");  // Paraziții
        seedPerformance("118007099", "Alchemy Stage", "2026-08-07T23:15:00+03:00", "2026-08-08T00:45:00+03:00");  // Pendulum (DJ Set)


        // Day 3: Saturday, Aug 8, 2026 - Main Stage
        seedPerformance("335403205", "Main Stage", "2026-08-08T20:00:00+03:00", "2026-08-08T21:30:00+03:00");  // Tujamo
        seedPerformance("1213405916", "Main Stage", "2026-08-08T21:45:00+03:00", "2026-08-08T23:15:00+03:00"); // Lewis Capaldi
        seedPerformance("877792512", "Main Stage", "2026-08-08T23:30:00+03:00", "2026-08-09T01:00:00+03:00");  // Lost Frequencies
        seedPerformance("430932944", "Main Stage", "2026-08-09T01:15:00+03:00", "2026-08-09T03:00:00+03:00");  // Martin Garrix

        // Day 3: Saturday, Aug 8, 2026 - Galaxy Stage
        seedPerformance("200779145", "Galaxy Stage", "2026-08-08T21:00:00+03:00", "2026-08-08T23:00:00+03:00"); // Solomun
        seedPerformance("258951272", "Galaxy Stage", "2026-08-08T23:00:00+03:00", "2026-08-09T01:30:00+03:00"); // Joseph Capriati
        seedPerformance("1636676418", "Galaxy Stage", "2026-08-09T01:30:00+03:00", "2026-08-09T04:00:00+03:00"); // Mau P

        // Day 3: Saturday, Aug 8, 2026 - Daydreaming Stage
        seedPerformance("697739898", "Daydreaming Stage", "2026-08-08T21:00:00+03:00", "2026-08-09T00:00:00+03:00"); // Shimza

        // Day 3: Saturday, Aug 8, 2026 - Alchemy Stage
        seedPerformance("631440154", "Alchemy Stage", "2026-08-08T21:00:00+03:00", "2026-08-08T22:30:00+03:00"); // Denzel Curry
        seedPerformance("288242555", "Alchemy Stage", "2026-08-08T22:45:00+03:00", "2026-08-09T00:15:00+03:00"); // Camo & Krooked


        // Day 4: Sunday, Aug 9, 2026 - Main Stage
        seedPerformance("255303209", "Main Stage", "2026-08-09T19:30:00+03:00", "2026-08-09T20:45:00+03:00");  // Flo Rida
        seedPerformance("1449138676", "Main Stage", "2026-08-09T21:00:00+03:00", "2026-08-09T22:15:00+03:00"); // Meduza Live
        seedPerformance("1062085272", "Main Stage", "2026-08-09T22:30:00+03:00", "2026-08-09T23:45:00+03:00"); // Alan Walker
        seedPerformance("331900515", "Main Stage", "2026-08-10T00:00:00+03:00", "2026-08-10T01:30:00+03:00");  // R3hab
        seedPerformance("211210469", "Main Stage", "2026-08-10T01:30:00+03:00", "2026-08-10T03:00:00+03:00");  // Afrojack
        seedPerformance("271066694", "Main Stage", "2026-08-10T03:15:00+03:00", "2026-08-10T04:45:00+03:00");  // Steve Aoki

        // Day 4: Sunday, Aug 9, 2026 - Galaxy Stage
        seedPerformance("1386592541", "Galaxy Stage", "2026-08-09T21:00:00+03:00", "2026-08-09T23:00:00+03:00"); // Célavi
        seedPerformance("16013761", "Galaxy Stage", "2026-08-09T23:00:00+03:00", "2026-08-10T01:00:00+03:00");   // Jamie Jones
        seedPerformance("1332641349", "Galaxy Stage", "2026-08-10T01:00:00+03:00", "2026-08-10T03:30:00+03:00"); // Sara Landry

        // Day 4: Sunday, Aug 9, 2026 - Daydreaming Stage
        seedPerformance("468500851", "Daydreaming Stage", "2026-08-09T21:00:00+03:00", "2026-08-09T23:00:00+03:00"); // Jan Blomqvist

        // Day 4: Sunday, Aug 9, 2026 - Alchemy Stage
        seedPerformance("111544331", "Alchemy Stage", "2026-08-09T21:00:00+03:00", "2026-08-09T23:00:00+03:00");  // Andy C

        System.out.println("Seeding of official UNTOLD 2026 performance schedule completed successfully.");
        seedStaffMembers();
    }

    private void seedPerformance(String artistSpotifyId, String stageName, String startTimeStr, String endTimeStr) {
        Artist artist = artistRepository.findBySpotifyId(artistSpotifyId).orElse(null);
        if (artist == null) {
            System.err.println("Warning: Could not seed performance because artist with Spotify ID " + artistSpotifyId + " was not found in DB.");
            return;
        }

        MapPin stage = mapPinRepository.findAll().stream()
                .filter(p -> "STAGE".equalsIgnoreCase(p.getType()) && stageName.equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElse(null);
        if (stage == null) {
            System.err.println("Warning: Could not seed performance because stage '" + stageName + "' was not found in DB.");
            return;
        }

        OffsetDateTime start = OffsetDateTime.parse(startTimeStr);
        OffsetDateTime end = OffsetDateTime.parse(endTimeStr);

        Performance performance = Performance.builder()
                .artist(artist)
                .stage(stage)
                .startTime(start)
                .endTime(end)
                .status(PerformanceStatus.SCHEDULED)
                .build();

        performanceRepository.save(performance);
        System.out.println("Seeded performance: " + artist.getName() + " on " + stage.getName() + " (" + startTimeStr + " - " + endTimeStr + ")");
    }

    private void seedStaffMembers() {
        if (staffMemberRepository.count() > 0) {
            System.out.println("Staff members already seeded.");
            return;
        }

        System.out.println("Seeding default Untold 2026 key command roster...");
        
        User alexUser = seedStaffUser("alex@untold.com", "alex123", "Alex Rivera", UserType.ADMIN);
        User marcusUser = seedStaffUser("marcus@untold.com", "marcus123", "Marcus Vance", UserType.ADMIN);
        User sarahUser = seedStaffUser("sarah@untold.com", "sarah123", "Sarah Jenkins", UserType.ADMIN);
        User danielUser = seedStaffUser("daniel@untold.com", "daniel123", "Daniel Kim", UserType.ADMIN);
        
        User elenaUser = seedStaffUser("elena@untold.com", "elena123", "Elena Popescu", UserType.ADMIN);
        User vladUser = seedStaffUser("vlad@untold.com", "vlad123", "Vlad Dumitrescu", UserType.ADMIN);
        User mihaiUser = seedStaffUser("mihai@untold.com", "mihai123", "Mihai Radu", UserType.ADMIN);
        User anaUser = seedStaffUser("ana@untold.com", "ana123", "Ana Sandu", UserType.ADMIN);
        User christianUser = seedStaffUser("christian@untold.com", "christian123", "Christian Bauer", UserType.ADMIN);
        User lauraUser = seedStaffUser("laura@untold.com", "laura123", "Laura Matei", UserType.ADMIN);
        User georgeUser = seedStaffUser("george@untold.com", "george123", "George Ionescu", UserType.ADMIN);
        User soniaUser = seedStaffUser("sonia@untold.com", "sonia123", "Sonia Dragomir", UserType.ADMIN);
        User davidUser = seedStaffUser("david@untold.com", "david123", "David Miller", UserType.ADMIN);

        createStaff("Alex Rivera", "Lead Organizer", "Operations", "Main Control Room", "Active", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=256&auto=format&fit=crop", alexUser);
        createStaff("Marcus Vance", "Stage Coordinator", "Production", "Main Stage Backstage", "Active", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=256&auto=format&fit=crop", marcusUser);
        createStaff("Sarah Jenkins", "Security Manager", "Security & Access", "Cluj Arena Gate 1", "Standby", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=256&auto=format&fit=crop", sarahUser);
        createStaff("Daniel Kim", "IT Support Director", "Infrastructure", "Techno Room B", "On Break", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=256&auto=format&fit=crop", danielUser);
        
        // Let's seed more staff members for generic table rendering & pagination demo
        createStaff("Elena Popescu", "Operations Officer", "Operations", "Cluj Arena Gate 1", "Active", "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?q=80&w=256&auto=format&fit=crop", elenaUser);
        createStaff("Vlad Dumitrescu", "Medical Team Lead", "Medical", "First Aid Hub 3", "Active", "https://images.unsplash.com/photo-1622253692010-333f2da6031d?q=80&w=256&auto=format&fit=crop", vladUser);
        createStaff("Mihai Radu", "Access Steward", "Security & Access", "Central Park East Entrance", "Offline", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?q=80&w=256&auto=format&fit=crop", mihaiUser);
        createStaff("Ana Sandu", "Logistics Assistant", "Operations", "Main Control Room", "Offline", "https://images.unsplash.com/photo-1580489944761-15a19d654956?q=80&w=256&auto=format&fit=crop", anaUser);
        createStaff("Christian Bauer", "Sound Engineer", "Production", "Main Stage Backstage", "Standby", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=256&auto=format&fit=crop", christianUser);
        createStaff("Laura Matei", "First Aid Responder", "Medical", "First Aid Hub 3", "Offline", "https://images.unsplash.com/photo-1567532939604-b6b5b0db2604?q=80&w=256&auto=format&fit=crop", lauraUser);
        createStaff("George Ionescu", "Network Administrator", "Infrastructure", "Main Control Room", "Active", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=256&auto=format&fit=crop", georgeUser);
        createStaff("Sonia Dragomir", "Crowd Marshal", "Security & Access", "Cluj Arena Gate 1", "Offline", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=256&auto=format&fit=crop", soniaUser);
        createStaff("David Miller", "Power Grid Engineer", "Infrastructure", "Techno Room B", "On Break", "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=256&auto=format&fit=crop", davidUser);

        System.out.println("Untold 2026 key command roster seeded successfully.");
    }

    private User seedStaffUser(String email, String password, String name, UserType type) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setUsername(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setUserType(type);
            User registered = authService.register(user);
            System.out.println("Seeded staff user: " + email);
            return registered;
        });
    }

    private void createStaff(String name, String role, String dept, String zone, String status, String avatar, User user) {
        StaffMember staff = new StaffMember();
        staff.setName(name);
        staff.setRole(role);
        staff.setDept(dept);
        staff.setZone(zone);
        staff.setStatus(status);
        staff.setAvatar(avatar);
        staff.setUser(user);
        staffMemberRepository.save(staff);
    }
}

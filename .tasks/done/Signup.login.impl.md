During signup, we create both Hotel + User.
The email/password entered by the person belongs to the User, not the Hotel.
The User is linked to the newly created Hotel through hotel_id.

So the flow is:
Frontend Signup
      ↓
POST /api/v1/auth/signup
      ↓
AuthController
      ↓
AuthService
      ↓
BEGIN TRANSACTION
      ↓
Create Hotel
      ↓
Create User
      ↓
user.hotel_id = hotel.id
      ↓
COMMIT
      ↓
JWT/Login response
      ↓
Frontend Dashboard
Below is the abstract backend structure.
1. Database Tables
We need only two tables for this first flow.
hotels
CREATE TABLE hotels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
Notice:
No email and no password here.
users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    hotel_id BIGINT NOT NULL,

    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),

    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels(id)
);
The important relationship is:
users.hotel_id → hotels.id
2. Example Database State
After signup:
hotels
id | name
---+-------------------
1  | Grand Palace Hotel
users
id | hotel_id | name    | email              | role
---+----------+---------+--------------------+-------
1  | 1        | Nagaraju| nagaraju@gmail.com | OWNER
So:
Hotel #1
   │
   └── User #1
         Nagaraju
         OWNER
3. Frontend Signup
Your existing frontend has:
/signup
User enters:
{
    "hotelName": "Grand Palace Hotel",
    "ownerName": "Nagaraju",
    "email": "nagaraju@gmail.com",
    "password": "********",
    "phone": "9876543210",
    "city": "Hyderabad"
}
Notice:
email = USER EMAIL
There is no hotel email in this model.
4. Signup Endpoint
POST /api/v1/auth/signup
Content-Type: application/json
Request:
{
    "hotelName": "Grand Palace Hotel",
    "ownerName": "Nagaraju",
    "email": "nagaraju@gmail.com",
    "password": "********",
    "phone": "9876543210",
    "city": "Hyderabad",
    "state": "Telangana",
    "country": "India",
    "postalCode": "500001"
}
5. DTO
Spring Boot:
public class SignupRequest {

    private String hotelName;

    private String ownerName;

    private String email;

    private String password;

    private String phone;

    private String city;

    private String state;

    private String country;

    private String postalCode;
}
The frontend sends this DTO.
6. Controller
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody SignupRequest request) {

        return ResponseEntity.ok(
                authService.signup(request)
        );
    }
}
So the connection is:
Frontend
   |
   | POST /api/v1/auth/signup
   ↓
AuthController.signup()
   |
   ↓
AuthService.signup()


you have to use coding principles and starties from d:\resource\Projects\atithi-auth\src\main\java\com\nag\atithiauth 

and D:\resource\Projects\atithi-gateway\src\main\java\com\nag\atithigateway  for authentication and user creations/jwt generation and verifying jwt tokens usinf rsa key pair (I have already place keys in D:\resource\Projects\Hotel Management\atithi-server\src\main\resources\keys).

For jwt generation you can use code from D:\resource\Projects\atithi-auth\src\main\java\com\nag\atithiauth\services\JwtService.java:

@Service
@RequiredArgsConstructor
public class JwtService {

    private final KeyLoader keyLoader;

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(60 * 15);

        return Jwts.builder()
                .claims(prepareClaims(user))
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(keyLoader.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private Map<String, Object> prepareClaims(User user) {
        return Map.of(
                "email", user.getEmail(),
                "roles", user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .toList()
        );
    }
}


For user & userprincipal you can look at :D:\resource\Projects\atithi-auth\src\main\java\com\nag\atithiauth\model\User.java &  D:\resource\Projects\atithi-auth\src\main\java\com\nag\atithiauth\model\UserPrincipal.java & D:\resource\Projects\atithi-auth\src\main\java\com\nag\atithiauth\model\BaseEntity.java

And for security configuration you can look at: D:\resource\Projects\atithi-auth\src\main\java\com\nag\atithiauth\config\SecurityConfig.java

And for jwt verification:
D:\resource\Projects\atithi-gateway\src\main\java\com\nag\atithigateway\filters\JwtAuthenticationFilter.java and cookie creation: D:\resource\Projects\atithi-gateway\src\main\java\com\nag\atithigateway\service\CookieService.java

I want both generation and validation in this one service only i.e atithi-server this project is different from other two atithi-auth & atithi-gateway those are for hostel management but this project is for hotel managemenet.

Implement code in a robust way and production grade. I need clear implmenetation of login and sigup. I didn't defined the User & UserPrincipal define them based on the field we have in this file. and also properly use userprincipal for AuthenticationManager or Usernamepassowrd authentication. 

RUle of completion:

-> Login
-> Signup (Hotel creation + User creation)
-> Login (should receive success mesg and jwt token should be stored in http only cookies).

-> Use lombok/jjwt libraries you can find in atithi-auth/pom.xml & atithi-gateway/pom.xml
-> use records whenever required
-> Use inheritance for model with BaseEntity for timestamps like createdAt etc..

-> Put Active states in EnUMs and use them in User & Hotel
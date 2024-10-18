### Change log

**Summary:**  
Add user model into domain. Removing deprecated account functionality  

**Project:** DataHub  
**Component:** Backend  
**Branch:** feature/01

---
Operation: `Create`  
Resource: `src/main/java/api/model/User.java`  
Content:

```java
public class User {
    private UUID id;
    private String name;
    private String email;
}
```

---
Operation: `Update`  
Resource: `src/main/java/service/UserService.java`  
fromLine: 24  
toLine: 42  
Content:

```java
/**
 * Fetches a user by their email address.
 * @param email The email of the user to be fetched.
 * @return User object if found, null otherwise.
 */
public User getUserByEmail(String email) {
    return userRepository.findByEmail(email);
}
  ```

---
Operation: `Delete`  
Resource: `src/main/java/service/AccountService.java`
---

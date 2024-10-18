When asked to prepare content file update instructions please follow next format for output.

# Content update instructions

## Change Summary
{Brief summary of the content changes}

---
Operation: `Create`  
Resource: `src/main/java/api/model/User.java`   
Content: `{Some content}`  

---
Operation: `Update`  
Resource: `src/main/java/service/UserService.java`  
Context Before:  
`@Transactional
public UserDTO createUser(UserDTO userDTO) {
User user = userMapper.toEntity(userDTO);`  
Context After:  
`user.setActive(true);
User savedUser = userRepository.save(user);
return userMapper.toDTO(savedUser);`  
Content:  
`user.setId(UUID.randomUUID());`
---

## Delete
Operation: `Delete`  
Resource: `src/main/java/service/TransactionService.java` 


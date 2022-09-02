package com.ahamed.abdullah.tweetapp.security;

import com.ahamed.abdullah.tweetapp.model.*;
import com.ahamed.abdullah.tweetapp.repository.TokenRepository;
import com.ahamed.abdullah.tweetapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class AuthenticationController {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private CustomUserDetailsManager customUserDetailsManager;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @GetMapping("username/{key}")
    public ResponseEntity<Boolean> findIfUsernameExist(@PathVariable String key){
        return ResponseEntity.ok().body(userRepository.existsByUsername(key));
    }

    @PostMapping("login")
    public ResponseEntity login(@RequestBody @Valid LoginRequest loginRequest) {
        try {
            Authentication authenticate = authenticationManager
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getUsername(), loginRequest.getPassword()
                            )
                    );
            UserDetails user = (CustomUserDetails) authenticate.getPrincipal();

            Token refreshToken = new Token(user.getUsername(), UUID.randomUUID().toString());
            tokenRepository.save(refreshToken);

            String token = jwtTokenProvider.createToken(user.getUsername());
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("refresh-token", refreshToken.getToken());
            responseBody.put("access-token", token);
            return ResponseEntity.ok().body(responseBody);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


    @PostMapping("register")
    public ResponseEntity<?> signup(@RequestBody @Valid User userRequest) throws RuntimeException {
        User newUser = new User();
        newUser.setUsername(userRequest.getUsername());
        newUser.setPassword(userRequest.getPassword());
        newUser.setResetKey(userRequest.getResetKey());
        newUser.setFirstName(userRequest.getFirstName());
        newUser.setLastName(userRequest.getLastName());
        newUser.setEmail(userRequest.getEmail());
        CustomUserDetails user = new CustomUserDetails(newUser);
        customUserDetailsManager.createUser(user);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("changePassword")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        customUserDetailsManager.changePassword(changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("{username}/forget")
    public ResponseEntity<?> forgetPassword(@PathVariable String username,@RequestBody ForgetPasswordRequest forgetPasswordRequest) throws Exception{
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if(optionalUser.isEmpty()){
            throw new Exception("user doesnot exists");
        }
        User user = optionalUser.get();
        if(!user.getResetKey().equals(forgetPasswordRequest.getResetKey())){
            throw new Exception("wrong reset key");
        }
        user.setPassword(passwordEncoder.encode(forgetPasswordRequest.getPassword()));
        tokenRepository.deleteAllByUsername(username);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }


    @GetMapping("sign-out")
    public ResponseEntity<?> signout(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        tokenRepository.deleteByToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("invalidate-all-access-token")
    public ResponseEntity<?> invalidateAllAccessToken(@RequestBody @Valid InvalidateAllAccessTokenRequest invalidateAllAccessTokenRequest) {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        UserDetails user = customUserDetailsManager.loadUserByUsername(currentUser.getName());
        try {
            authenticationManager
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    user.getUsername(), invalidateAllAccessTokenRequest.getPassword()
                            )
                    );
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().build();
        }

        tokenRepository.deleteAllByUsername(user.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping ("createAccessToken")
    public ResponseEntity<String> createAccessToken(@RequestBody @Valid RefreshTokenRequest refreshToken) {
        Optional<Token> optionalToken = tokenRepository.findByToken(refreshToken.getRefreshToken());
        if (optionalToken.isEmpty()) {
            return ResponseEntity.badRequest().body("unauthorized");
        }
        UserDetails userDetails = customUserDetailsManager.loadUserByUsername(optionalToken.get().getUsername());
        String token = jwtTokenProvider.createToken(userDetails.getUsername());

        return ResponseEntity.ok().body(token);
    }


}

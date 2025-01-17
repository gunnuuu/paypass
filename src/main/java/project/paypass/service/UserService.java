package project.paypass.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.paypass.domain.User;
import project.paypass.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public boolean checkNewUser(String mainId){
        return userRepository.findByMainId(mainId).isEmpty();
    }

    @Transactional
    public void save(User user){
        userRepository.save(user);
    }

    @Transactional
    public User findById(Long id){
        return userRepository.findById(id).get();
    }

    @Transactional
    public Long findIdByMainId(String mainId){
        return userRepository.findByMainId(mainId).get(0).getId();
    }
}
package ch.admin.bit.jeap.jme.cdct.consumer.web.api;

import ch.admin.bit.jeap.jme.cdct.consumer.infrastructure.user.User;
import ch.admin.bit.jeap.jme.cdct.consumer.infrastructure.user.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@Slf4j
@RequiredArgsConstructor
class UserGatewayController {

    private final UserClient userClient;

    @GetMapping()
    public List<User> getAllUsers() {
        return userClient.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") String id) {
        return userClient.getUserById(id);
    }

}

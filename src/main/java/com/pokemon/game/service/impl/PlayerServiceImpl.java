package com.pokemon.game.service.impl;

import com.pokemon.game.model.Player;
import com.pokemon.game.model.User;
import com.pokemon.game.repository.PlayerRepository;
import com.pokemon.game.repository.UserRepository;
import com.pokemon.game.service.IPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 *
 * Luis
 */
@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements IPlayerService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;

    @Override
    public User getCurrentAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Override
    public Player getCurrentPlayer() {
        User currentUser = getCurrentAuthenticatedUser();
        return playerRepository.findByUser(currentUser)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado para el usuario: " + currentUser.getUsername()));
    }

    @Override
    public Player findOrCreatePlayerForUser(User user) {
        return playerRepository.findByUser(user).orElseGet(() -> {
            Player newPlayer = Player.builder()
                    .user(user)
                    .build();
            return playerRepository.save(newPlayer);
        });
    }
}

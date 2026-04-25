package com.hrflow.direction.service;

import com.hrflow.direction.specs.DirectionSpecification;
import com.hrflow.direction.dto.DirectionRequest;
import com.hrflow.direction.dto.DirectionResponse;
import com.hrflow.direction.dto.DirectionSearchDto;
import com.hrflow.direction.entities.Direction;
import com.hrflow.direction.exception.DirecteurRoleException;
import com.hrflow.direction.exception.DirectionNomConflictException;
import com.hrflow.direction.exception.DirectionNotFoundException;
import com.hrflow.direction.mapper.DirectionMapper;
import com.hrflow.direction.repositories.DirectionRepository;
import com.hrflow.users.entities.User;
import com.hrflow.users.exception.UserNotFoundException;
import com.hrflow.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DirectionService {

    private static final Logger log = LoggerFactory.getLogger(DirectionService.class);
    private static final String ROLE_DIRECTEUR = "DIRECTEUR";

    private final DirectionRepository directionRepository;
    private final DirectionMapper     directionMapper;
    private final UserRepository      userRepository;

    public DirectionService(DirectionRepository directionRepository,
                            DirectionMapper directionMapper,
                            UserRepository userRepository) {
        this.directionRepository = directionRepository;
        this.directionMapper     = directionMapper;
        this.userRepository      = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<DirectionResponse> search(DirectionSearchDto search, Pageable pageable) {
        Page<Direction> page = directionRepository
                .findAll(DirectionSpecification.fromSearch(search), pageable);

        // Une seule requête COUNT pour toute la page — évite N+1
        List<Long> ids = page.stream().map(Direction::getId).toList();
        Map<Long, Long> countMap = directionRepository
                .countFichesDePosteByDirectionIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        return page.map(d -> toResponseWithCount(d, countMap.getOrDefault(d.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public DirectionResponse findById(Long id) {
        Direction direction = directionRepository.findWithDirecteurById(id)
                .orElseThrow(() -> new DirectionNotFoundException(id));
        return toResponseWithCount(direction);
    }

    @Transactional
    public DirectionResponse create(DirectionRequest request) {
        if (directionRepository.existsByNom(request.nom())) {
            throw new DirectionNomConflictException(request.nom());
        }
        Direction direction = new Direction();
        directionMapper.updateEntity(request, direction);
        assignDirecteur(direction, request.directeurId());
        Direction saved = directionRepository.save(direction);
        log.info("Direction créée : id={}, nom={}", saved.getId(), saved.getNom());
        return toResponseWithCount(saved);
    }

    @Transactional
    public DirectionResponse update(Long id, DirectionRequest request) {
        Direction direction = directionRepository.findWithDirecteurById(id)
                .orElseThrow(() -> new DirectionNotFoundException(id));

        if (directionRepository.existsByNomAndIdNot(request.nom(), id)) {
            throw new DirectionNomConflictException(request.nom());
        }
        directionMapper.updateEntity(request, direction);
        assignDirecteur(direction, request.directeurId());
        Direction saved = directionRepository.save(direction);
        log.info("Direction mise à jour : id={}", saved.getId());
        return toResponseWithCount(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!directionRepository.existsById(id)) {
            throw new DirectionNotFoundException(id);
        }
        directionRepository.deleteById(id);
        log.info("Direction supprimée : id={}", id);
    }

    private void assignDirecteur(Direction direction, Long directeurId) {
        User user = userRepository.findWithRolesById(directeurId)
                .orElseThrow(() -> new UserNotFoundException(directeurId));

        boolean hasRole = user.getRoles().stream()
                .anyMatch(r -> ROLE_DIRECTEUR.equals(r.getRoleName()));

        if (!hasRole) {
            throw new DirecteurRoleException(directeurId);
        }
        direction.setDirecteur(user);
    }

    /** Utilisé par findById / create / update — count individuel acceptable. */
    private DirectionResponse toResponseWithCount(Direction direction) {
        DirectionResponse base = directionMapper.toResponse(direction);
        long count = directionRepository.countFichesDePoste(direction.getId());
        return new DirectionResponse(
                base.id(), base.nom(),
                base.directeurId(), base.directeurNom(),
                count,
                base.createdAt(), base.updatedAt()
        );
    }

    /** Utilisé par search() — count déjà calculé en bulk. */
    private DirectionResponse toResponseWithCount(Direction direction, long fichesCount) {
        DirectionResponse base = directionMapper.toResponse(direction);
        return new DirectionResponse(
                base.id(), base.nom(),
                base.directeurId(), base.directeurNom(),
                fichesCount,
                base.createdAt(), base.updatedAt()
        );
    }
}

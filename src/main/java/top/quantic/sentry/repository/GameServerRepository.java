package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import top.quantic.sentry.domain.GameServer;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Spring Data MongoDB repository for the GameServer entity.
 */
@SuppressWarnings("unused")
public interface GameServerRepository extends MongoRepository<GameServer, String> {

    Optional<GameServer> findById(String id);

    Optional<GameServer> findByAddress(String address);

    Optional<GameServer> findByAddressStartingWith(String address);

    @Query("{ $where : 'this.rcon_password == null || (this.last_rcon_date <= this.expiration_date && this.expiration_date <= new Date())' }")
    List<GameServer> findMissingOrExpiredRcon();

    Stream<GameServer> findByStatusCheckDateBefore(ZonedDateTime dateTime);

    List<GameServer> findByVersionLessThan(Integer version);

    Stream<GameServer> findByLastGameUpdateBeforeAndVersionLessThan(ZonedDateTime date, Integer version);

    List<GameServer> findByPingLessThanEqual(Integer ping);

    List<GameServer> findByPingGreaterThan(Integer ping);

    List<GameServer> findByRconPasswordIsNull();

    List<GameServer> findByUpdatingIsTrueAndUpdateAttemptsGreaterThan(Integer threshold);

    List<GameServer> findByIdIn(Collection<String> ids);
}

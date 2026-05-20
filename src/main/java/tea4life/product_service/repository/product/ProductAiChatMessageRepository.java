package tea4life.product_service.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tea4life.product_service.model.ProductAiChatMessage;

import java.time.Instant;
import java.util.List;

public interface ProductAiChatMessageRepository extends JpaRepository<ProductAiChatMessage, Long> {

    long countByActiveTrue();

    long countByCreatedAtGreaterThanEqualAndActiveTrue(Instant startTime);

    long countByUserKeycloakIdAndCreatedAtGreaterThanEqualAndActiveTrue(String userKeycloakId, Instant startTime);

    @Query("""
            select m
            from ProductAiChatMessage m
            where m.active = true
              and m.userKeycloakId = :userKeycloakId
            order by m.createdAt desc
            """)
    Page<ProductAiChatMessage> findRecentByUserKeycloakId(
            @Param("userKeycloakId") String userKeycloakId,
            Pageable pageable
    );

    @Query("""
            select count(distinct m.userKeycloakId)
            from ProductAiChatMessage m
            where m.active = true
              and m.userKeycloakId is not null
              and m.userKeycloakId <> ''
            """)
    long countDistinctKnownUsers();

    interface TopQuestionProjection {
        String getQuestion();

        Long getQuestionCount();
    }

    @Query(
            value = """
                    select min(question) as question,
                           count(*) as questionCount
                    from product_ai_chat_messages
                    where active = true
                      and normalized_question is not null
                      and normalized_question <> ''
                    group by normalized_question
                    order by questionCount desc
                    """,
            nativeQuery = true
    )
    List<TopQuestionProjection> findTopQuestions(Pageable pageable);

    interface UserUsageProjection {
        String getUserKeycloakId();

        String getUserEmail();

        Long getQuestionCount();

        Long getTodayQuestionCount();

        Instant getLastAskedAt();
    }

    interface MonthlyQuestionProjection {
        Integer getMonth();

        Long getQuestionCount();
    }

    @Query(
            value = """
                    select extract(month from created_at) as month,
                           count(*) as questionCount
                    from product_ai_chat_messages
                    where active = true
                      and created_at >= :startOfYear
                      and created_at < :startOfNextYear
                    group by extract(month from created_at)
                    order by month
                    """,
            nativeQuery = true
    )
    List<MonthlyQuestionProjection> findMonthlyQuestionStats(
            @Param("startOfYear") Instant startOfYear,
            @Param("startOfNextYear") Instant startOfNextYear
    );

    @Query(
            value = """
                    select coalesce(user_keycloak_id, 'anonymous') as userKeycloakId,
                           coalesce(user_email, '') as userEmail,
                           count(*) as questionCount,
                           sum(case when created_at >= :startOfDay then 1 else 0 end) as todayQuestionCount,
                           max(created_at) as lastAskedAt
                    from product_ai_chat_messages
                    where active = true
                      and (:emailKeyword is null or :emailKeyword = '' or lower(coalesce(user_email, '')) like concat('%', lower(:emailKeyword), '%'))
                      and (:fromTime is null or created_at >= :fromTime)
                      and (:toTime is null or created_at <= :toTime)
                    group by coalesce(user_keycloak_id, 'anonymous'), coalesce(user_email, '')
                    order by lastAskedAt desc
                    """,
            countQuery = """
                    select count(*)
                    from (
                        select coalesce(user_keycloak_id, 'anonymous') as userKeycloakId,
                               coalesce(user_email, '') as userEmail
                        from product_ai_chat_messages
                        where active = true
                          and (:emailKeyword is null or :emailKeyword = '' or lower(coalesce(user_email, '')) like concat('%', lower(:emailKeyword), '%'))
                          and (:fromTime is null or created_at >= :fromTime)
                          and (:toTime is null or created_at <= :toTime)
                        group by coalesce(user_keycloak_id, 'anonymous'), coalesce(user_email, '')
                    ) grouped_users
                    """,
            nativeQuery = true
    )
    Page<UserUsageProjection> findUserUsageStats(
            @Param("startOfDay") Instant startOfDay,
            @Param("emailKeyword") String emailKeyword,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );
}

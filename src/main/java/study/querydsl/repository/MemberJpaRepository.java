package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m",Member.class).getResultList();
    }

    public Member findByUsername(String username){
        return em.createQuery("select m from Member m where m.username = :username",Member.class)
                .setParameter("username",username)
                .getSingleResult();
    }

    public List<Member> findAll_querydsl(){
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername_querydsl(String username){
        return queryFactory.selectFrom(member)
                .where(member.username.eq(username)).fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition memCond){
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(memCond.getUsername())) {
            builder.and(member.username.eq(memCond.getUsername()));
        }
        if (hasText(memCond.getTeamName())) {
            builder.and(team.name.eq(memCond.getTeamName()));
        }
        if (memCond.getAgeGoe() != null) {
            builder.and(member.age.goe(memCond.getAgeGoe()));
        }
        if (memCond.getAgeLoe() != null) {
            builder.and(member.age.loe(memCond.getAgeLoe()));
        }
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team,team)
                .fetch();
    }
    public List<MemberTeamDto> search(MemberSearchCondition memCond){
        return  queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(memCond.getUsername()),
                        teamNameEq(memCond.getTeamName()),
                        ageGoe(memCond.getAgeGoe()),
                        ageLoe(memCond.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return isEmpty(username) ? null : member.username.eq(username);
    }
    private BooleanExpression teamNameEq(String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }
    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }
}

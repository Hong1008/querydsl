package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.repository.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;


//    public MemberRepositoryImpl() {
//        super(Member.class);
//    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition memCond) {
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition memCond, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();
        List<MemberTeamDto> result = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(result,pageable,total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition memCond, Pageable pageable) {
        List<MemberTeamDto> result = queryFactory
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
        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(memCond.getUsername()),
                        teamNameEq(memCond.getTeamName()),
                        ageGoe(memCond.getAgeGoe()),
                        ageLoe(memCond.getAgeLoe()))
                .fetchCount();

        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(memCond.getUsername()),
                        teamNameEq(memCond.getTeamName()),
                        ageGoe(memCond.getAgeGoe()),
                        ageLoe(memCond.getAgeLoe()));
        //return new PageImpl<>(result,pageable,total);
        return PageableExecutionUtils.getPage(result,pageable, countQuery::fetchCount);
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

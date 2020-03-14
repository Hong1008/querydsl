package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10 , teamA);
        Member member2 = new Member("member2", 20 , teamA);
        Member member3 = new Member("member3", 30 , teamB);
        Member member4 = new Member("member4", 40 , teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception{
        //given


        //when
        Member findByJpql = em.createQuery("select  m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        //then
        assertThat(findByJpql.getUsername()).isEqualTo("member1");

    }
    
    @Test
    public void startQuerydsl() throws Exception{
        //given

        //when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void search() throws Exception{
        //given
        
        //when
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1")
                        ,member.age.eq(10))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void resultFetchTest() throws Exception{
        //given
        
        //when
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();
        //단 건
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();
        //처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();
        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
        //then
    }
    
    @Test
    public void sort() throws Exception{
        //given
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        //when
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = findMembers.get(0);
        Member member6 = findMembers.get(1);
        Member memberNull = findMembers.get(2);
        //then
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }
    
    @Test
    public void paging() throws Exception{
        //given
        
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        //then
        assertThat(result.size()).isEqualTo(2);
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }
    
    @Test
    public void aggregation() throws Exception{
        //given
        
        //when
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg())
                .from(member)
                .fetch();

        List<Tuple> resultGroup = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple tuple = result.get(0);

        Tuple teamA = resultGroup.get(0);
        Tuple teamB = resultGroup.get(1);

        //then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    }

    @Test
    public void join() throws Exception{
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetch();

        assertThat(result)
                .extracting("username")
                .contains("member1", "member2");
    }

    @Test
    public void join_on() throws Exception{
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }
    
    @Test
    public void join_on_relation() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }
    
    @Test
    public void fetchJoin() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        Team team = findMember.getTeam();
        System.out.println("team.getName() = " + team.getName());

    }
    
    @Test
    public void subQuery() throws Exception{
        QMember memSub = new QMember("memSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memSub.age.max())
                                .from(memSub)
                )).fetch();

        for (Member member1 : result) {
            System.out.println("member1.getAge() = " + member1.getAge());
        }

        List<Member> resultIn = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memSub.age)
                                .from(memSub)
                                .where(memSub.age.gt(10))
                ))
                .fetch();

        for (Member member1 : resultIn) {
            System.out.println("member1.getAge() = " + member1.getAge());
        }

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        select(memSub.age.avg())
                                .from(memSub)
                ).from(member)
                .fetch();
        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " +
                    tuple.get(select(memSub.age.avg())
                            .from(memSub)));
        }
    }
    
    @Test
    public void caseQuery() throws Exception{
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception{
        Tuple result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetchFirst();

        String concat = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
    }
    
    @Test
    public void simpleProjection() throws Exception{

    }
    
    @Test
    public void findDtoQueryDsl() throws Exception{
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoAnno() throws Exception{
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_booleanBuilder() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam,ageParam);

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

    }

    @Test
    public void dynamicQuery_whereParam() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam,ageParam);

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),ageEq(ageCond))
                .fetch();

    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) :  null;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) :  null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    
    @Test
    public void bulkUpdate() throws Exception{
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.eq(28))
                .execute();
        long delete = queryFactory
                .delete(member)
                .where(member.age.eq(10))
                .execute();

        System.out.println("delete = " + delete);

    }
}

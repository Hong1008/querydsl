package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import java.util.List;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);
		em.flush();
		em.clear();

		JPAQueryFactory query = new JPAQueryFactory(em);
		List<Hello> list = query.select(QHello.hello)
				.from(QHello.hello)
				.fetch();
		System.out.println("list = " + list.get(0).getId());
	}

}

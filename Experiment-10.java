// Import statements
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.hibernate5.HibernateTransactionManager;

import java.util.Properties;

// =====================
// 1. ENTITY CLASSES
// =====================

@Entity
@Table(name = "students")
class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    public Student() {}
    public Student(String name) { this.name = name; }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@Entity
@Table(name = "accounts")
class Account {
    @Id
    private int id;
    private double balance;

    public Account() {}
    public Account(int id, double balance) { this.id = id; this.balance = balance; }

    public int getId() { return id; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

// =====================
// 2. DAO CLASSES
// =====================

@Repository
class StudentDAO {
    @PersistenceContext
    private EntityManager em;

    public void save(Student student) { em.persist(student); }
    public Student get(int id) { return em.find(Student.class, id); }
    public void update(Student student) { em.merge(student); }
    public void delete(Student student) { em.remove(student); }
}

@Repository
class AccountDAO {
    @PersistenceContext
    private EntityManager em;

    public Account getAccount(int id) { return em.find(Account.class, id); }
    public void updateAccount(Account account) { em.merge(account); }
}

// =====================
// 3. SERVICE CLASSES
// =====================

@Service
class BankService {

    private final AccountDAO accountDAO;

    public BankService(AccountDAO accountDAO) { this.accountDAO = accountDAO; }

    @Transactional
    public void transfer(int fromId, int toId, double amount) {
        Account from = accountDAO.getAccount(fromId);
        Account to = accountDAO.getAccount(toId);

        if (from.getBalance() < amount) {
            throw new RuntimeException("Insufficient funds");
        }

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        accountDAO.updateAccount(from);
        accountDAO.updateAccount(to);

        System.out.println("Transfer successful!");
    }
}

// =====================
// 4. SPRING CONFIGURATION
// =====================

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "com.example")
class AppConfig {

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();

        Properties props = new Properties();
        props.put(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
        props.put(Environment.URL, "jdbc:mysql://localhost:3306/testdb");
        props.put(Environment.USER, "root");
        props.put(Environment.PASS, "password");
        props.put(Environment.DIALECT, "org.hibernate.dialect.MySQLDialect");
        props.put(Environment.HBM2DDL_AUTO, "update");
        props.put(Environment.SHOW_SQL, "true");

        sessionFactory.setHibernateProperties(props);
        sessionFactory.setAnnotatedClasses(Student.class, Account.class);
        return sessionFactory;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

    @Bean
    public BankService bankService(AccountDAO accountDAO) {
        return new BankService(accountDAO);
    }
}

// =====================
// 5. MAIN APPLICATION
// =====================

public class MainApp {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // ----- Spring DI demonstration -----
        Student student = new Student("John Doe");
        System.out.println("Student created: " + student.getName());

        // ----- Hibernate CRUD demonstration -----
        StudentDAO studentDAO = context.getBean(StudentDAO.class);
        studentDAO.save(student);  // Create
        Student s = studentDAO.get(student.getId()); // Read
        System.out.println("Read Student: " + s.getName());
        s.setName("Jane Doe");
        studentDAO.update(s); // Update
        System.out.println("Updated Student: " + studentDAO.get(s.getId()).getName());
        studentDAO.delete(s); // Delete
        System.out.println("Student deleted");

        // ----- Transaction management demonstration -----
        AccountDAO accountDAO = context.getBean(AccountDAO.class);
        accountDAO.updateAccount(new Account(1, 1000));
        accountDAO.updateAccount(new Account(2, 500));

        BankService bankService = context.getBean(BankService.class);
        bankService.transfer(1, 2, 200); // Transfer money between accounts

        context.close();
    }
}

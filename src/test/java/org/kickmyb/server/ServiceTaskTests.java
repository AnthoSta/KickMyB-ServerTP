package org.kickmyb.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.kickmyb.transfer.HomeItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = KickMyBServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@ActiveProfiles("test")
class ServiceTaskTests {

    @Autowired
    private MUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceTask serviceTask;

    @Test
    void testAddTask() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);

        assertEquals(1, serviceTask.home(u.id).size());
    }

    @Test
    void testAddTaskEmpty()  {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Empty");
        } catch (Exception e) {
            assertEquals(ServiceTask.Empty.class, e.getClass());
        }
    }

    @Test
    void testAddTaskTooShort() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "o";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.TooShort");
        } catch (Exception e) {
            assertEquals(ServiceTask.TooShort.class, e.getClass());
        }
    }

    @Test
    void testAddTaskExisting() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Bonne tâche";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Existing");
        } catch (Exception e) {
            assertEquals(ServiceTask.Existing.class, e.getClass());
        }
    }

    //Test delete
    @Test
    void testDeleteCorrectID() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");

        userRepository.saveAndFlush(u);

        //Ajouter une tâche
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);
        //Vérifier qu'il a une tâche
        assertEquals(1, serviceTask.home(u.id).size());
        AddTaskRequest atr2 = new AddTaskRequest();

        //atr2.name = "24545343";
        //atr2.deadline = Date.from(new Date().toInstant().plusSeconds(3800));
        //serviceTask.addOne(atr2, u);

        MUser leUser = userRepository.findByUsername("M. Test").get();
        //,leUser.tasks.getFirst().id
        HomeItemResponse task = serviceTask.home(leUser.id).getFirst();

        serviceTask.taskDelete(leUser,task.id);

        //Verifier que la tâche a été supprimé
        assertEquals(0, serviceTask.home(leUser.id).size());
    }
    @Test
    void testDeleteIncorrectID() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");

        userRepository.saveAndFlush(u);

        //Ajouter une tâche
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);
        //Vérifier qu'il a une tâche
        assertEquals(1, serviceTask.home(u.id).size());

        MUser leUser = userRepository.findByUsername("M. Test").get();
        HomeItemResponse task = serviceTask.home(leUser.id).getFirst();
        try{
            serviceTask.taskDelete(leUser,task.id + 1);
            fail("Aurait du lancer NoSuchElementException");
        }
        catch (Exception e){
            assertEquals(NoSuchElementException.class, e.getClass());
        }
    }

    @Test
    void testDeleteControleAcces() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        //Création du User Alice
        MUser alice = new MUser();
        alice.username = "Alice";
        alice.password = passwordEncoder.encode("Passw0rd!");

        userRepository.saveAndFlush(alice);

        //Ajouter une tâche
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Une tache";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, alice);
        //Vérifier qu'il a une tâche
        assertEquals(1, serviceTask.home(alice.id).size());

        //Création du user Bob
        MUser bob = new MUser();
        bob.username = "Bob";
        bob.password = passwordEncoder.encode("Passw0rd!");

        userRepository.saveAndFlush(bob);

        MUser leUser = userRepository.findByUsername("Alice").get();
        HomeItemResponse taskAlice = serviceTask.home(leUser.id).getFirst();
        MUser leBob = userRepository.findByUsername("Bob").get();
        try{
            serviceTask.taskDelete(leBob,taskAlice.id);
            fail("Aurait du lancer ????");
        }
        catch (Exception e){
            assertEquals(NoSuchElementException.class, e.getClass());
        }
    }
}

package me.rudrade.todo.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.RollbackException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
            .setControllerAdvice(new ExceptionHandler())
            .build();
    }

    @Test
    void itShouldHandleDataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/ex/data-integrity"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid data sent"));
    }

    @Test
    void itShouldHandleConstraintViolation() throws Exception {
        mockMvc.perform(get("/ex/constraint"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid data sent"));
    }

    @Test
    void itShouldHandleTransactionSystemException() throws Exception {
        mockMvc.perform(get("/ex/tx"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid data sent"));
    }

    @Test
    void itShouldHandleInvalidDataException() throws Exception {
        mockMvc.perform(get("/ex/invalid-data"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("bad-data"));
    }

    @Test
    void itShouldHandleEntityAlreadyExists() throws Exception {
        mockMvc.perform(get("/ex/entity-exists"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("exists"));
    }

    @Test
    void itShouldHandleInvalidAccess() throws Exception {
        mockMvc.perform(get("/ex/invalid-access"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access Invalid"));
    }

    @Test
    void itShouldHandleRuntimeException() throws Exception {
        mockMvc.perform(get("/ex/runtime"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void itShouldHandleMethodArgumentTypeMismatch() throws Exception {
        mockMvc.perform(get("/ex/path/not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid argument"))
            .andExpect(jsonPath("$.errors[0]").value("id"));
    }

    @RestController
    @RequestMapping("/ex")
    static class DummyController {
        @GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("bad");
        }

        @GetMapping("/constraint")
        void constraint() {
            throw new ConstraintViolationException("violation", java.util.Set.of());
        }

        @GetMapping("/tx")
        void tx() {
            var cve = new ConstraintViolationException("violation", java.util.Set.of());
            throw new TransactionSystemException("tx", new RollbackException(cve));
        }

        @GetMapping("/invalid-data")
        void invalidData() {
            throw new InvalidDataException("bad-data");
        }

        @GetMapping("/entity-exists")
        void entityExists() {
            throw new EntityAlreadyExistsException("exists");
        }

        @GetMapping("/invalid-access")
        void invalidAccess() {
            throw new InvalidAccessException();
        }

        @GetMapping("/runtime")
        void runtime() {
            throw new RuntimeException("boom");
        }

        @GetMapping("/path/{id}")
        void path(@PathVariable Integer id) {
            // no-op, mismatch triggers MethodArgumentTypeMismatchException
        }
    }
}


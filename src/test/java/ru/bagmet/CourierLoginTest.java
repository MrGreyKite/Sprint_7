package ru.bagmet;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.bagmet.data.CourierCredentials;

import java.util.Random;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.bagmet.data.StatusCodes.*;

@Tag("courier")
@DisplayName("Тесты на авторизацию курьера")
public class CourierLoginTest {

    CourierClient courierClient = new CourierClient();
    Random random = new Random();
    String login = "aNewExistingLogin";
    String password = "secret" + random.nextInt(1000);
    String firstName = "Курьер";
    CourierCredentials loginCredentials;

    @BeforeEach
    @Step("Создание тестового курьера для авторизации")
    public void createTestCourier(){
        loginCredentials = new CourierCredentials(login, password, firstName);
        courierClient.createCourier(loginCredentials);
    }

    @Test
    @DisplayName("Авторизация с корректным логином и паролем")
    @Description("Проверяется корректность авторизации существующим курьером через получение его ID")
    public void loginWithCorrectLoginAndPassword() {
        ValidatableResponse response = courierClient.loginWithCourier(loginCredentials);

        int courierID = response.extract().body().path("id");
        int statusCode = response.extract().statusCode();

        Allure.step("Проверка корректности данных в ответе по авторизации курьера", () -> {
            assertAll("Приходит правильный статус-код и айди курьера в виде целого числа",
                    () -> assertEquals(OK.getCode(), statusCode),
                    () -> assertThat(courierID, is(instanceOf(Integer.class)))
            );
        });
    }

    static Stream<Arguments> incorrectLoginData() {
        return Stream.of(
                arguments(new CourierCredentials("", ""), BAD_REQUEST.getCode(), "Недостаточно данных для входа"),
                arguments(new CourierCredentials("", "pass"), BAD_REQUEST.getCode(), "Недостаточно данных для входа"),
                arguments(new CourierCredentials("someLogin", ""), BAD_REQUEST.getCode(), "Недостаточно данных для входа"),
                arguments(new CourierCredentials("aNewExistingLogin", "non"), NOT_FOUND.getCode(), "Учетная запись не найдена"),
                arguments(new CourierCredentials("nonExistingLogin", "nonExistingPassword"), NOT_FOUND.getCode(), "Учетная запись не найдена")
        );
    }

    @ParameterizedTest(name = "Авторизация курьером {0} с некорректными данными приводит к ошибке с кодом {1} и текстом {2}")
    @MethodSource("incorrectLoginData")
    @DisplayName("Попытка авторизации некорректными данными")
    @Description("Проверяется авторизация с разными сочетаниями логина/пароля - отсутствующими и неверными")
    public void loginWithIncorrectData(CourierCredentials courier, int statusCode, String message) {
        ValidatableResponse response = courierClient.loginWithCourier(courier);

        int sCodeActual = response.extract().statusCode();
        String messageActual = response.extract().path("message");

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и ожидаемое сообщение об ошибке",
                    () -> assertEquals(statusCode, sCodeActual),
                    () -> assertEquals(message, messageActual)
            );
        });

    }

    @AfterEach
    public void tearDown(){
        Allure.step("Постусловие: удаление созданного курьера", () -> {
            if(loginCredentials != null) {
                ValidatableResponse resp = courierClient.loginWithCourier(loginCredentials);
                courierClient.deleteCourierByID(resp.extract().path("id"));
            }
        });
    }


}

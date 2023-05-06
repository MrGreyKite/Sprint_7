package ru.bagmet;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import ru.bagmet.data.CourierCredentials;
import ru.bagmet.data.StatusCodes;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static ru.bagmet.data.StatusCodes.BAD_REQUEST;
import static ru.bagmet.data.StatusCodes.CREATED;

@Tag("courier")
@DisplayName("Тесты на создание курьера")
public class CourierCreationTest {
    static CourierClient courierClient = new CourierClient();
    private CourierCredentials courier;
    Random random = new Random();
    String login = "johnny123";
    String password = "dodo321";
    String firstName = "Джонни";

    String alternateLogin = "peter789";
    String anotherPassword = "qwerty000";
    String anotherFirstName = "Тестин";

    @Test
    @DisplayName("Создание курьера со всеми параметрами")
    @Description("Проверяется возможность успешно создать курьера с логином, паролем и именем")
    public void courierWithFullValidDataCreation() {
        courier = new CourierCredentials(login, password, firstName);

        ValidatableResponse response = courierClient.createCourier(courier).spec(courierClient.getResponseSpecForCreated());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и подтверждается создание курьера",
                    () -> assertEquals(CREATED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("ok"), is(true))
            );
        });
    }

    @Test
    @DisplayName("Создание курьера только с логином и паролем")
    @Description("Проверяется невозможность создать курьера без указания имени")
    public void courierWithOnlyLoginAndPasswordCreation() {
        String log = "courier" + random.nextInt(1000); // генерируем случайное число от 0 до 999 и добавляем его к префиксу "courier"
        String pass = "pass" + random.nextInt(1000);

        courier = new CourierCredentials(log, pass);
        ValidatableResponse response = courierClient.createCourier(courier).spec(courierClient.getResponseSpecForGenericError());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Недостаточно данных для создания учетной записи",
                            response.extract().body().path("message"))
            );
        });
    }

    @Test
    @DisplayName("Создание курьера без пароля")
    @Description("Проверяется невозможность успешно создать курьера без указания пароля")
    public void courierWithoutPasswordCreation() {
        String log = "courier" + random.nextInt(1000);

        courier = new CourierCredentials();
        courier.setLogin(log);
        courier.setFirstName(firstName);

        ValidatableResponse response = courierClient.createCourier(courier).spec(courierClient.getResponseSpecForGenericError());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Недостаточно данных для создания учетной записи",
                            response.extract().body().path("message"))
            );
        });
    }

    @Test
    @DisplayName("Создание курьера без логина")
    @Description("Проверяется невозможность успешно создать курьера без указания логина")
    public void courierWithoutLoginCreation() {
        String pass = "pass" + random.nextInt(1000);

        courier = new CourierCredentials();
        courier.setPassword(pass);
        courier.setFirstName(firstName);

        ValidatableResponse response = courierClient.createCourier(courier).spec(courierClient.getResponseSpecForGenericError());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Недостаточно данных для создания учетной записи",
                            response.extract().body().path("message"))
            );
        });
    }

    @Test
    @DisplayName("Создание курьера, когда курьер с таким же логином уже существует")
    @Description("Проверяется невозможность успешно создать курьеров с одинаковым логином, но разными именами")
    public void courierWithNonUniqueLoginCreation(){
        courier = new CourierCredentials(alternateLogin, password, firstName);
        courierClient.createCourier(courier);

        CourierCredentials courier2 = new CourierCredentials(alternateLogin, anotherPassword, anotherFirstName);
        ValidatableResponse response = courierClient.createCourier(courier2);

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и сообщение об ошибке",
                    () -> assertEquals(StatusCodes.CONFLICT.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Этот логин уже используется",
                            response.extract().body().path("message"))
            );
        });
    }

    @AfterEach
    public void tearDown(){
        Allure.step("Постусловие: удаление созданного курьера", () -> {
            if(courier != null) {
                CourierCredentials loginCredentials = new CourierCredentials(courier.getLogin(), courier.getPassword());
                courierClient.deleteCourierByID(courierClient.loginWithCourier(loginCredentials).extract().body().path("id"));
            }
        });
        }
    }


package ru.bagmet;


import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.bagmet.data.OrderData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.bagmet.data.StatusCodes.BAD_REQUEST;
import static ru.bagmet.data.StatusCodes.CREATED;

@Tag("order")
@DisplayName("Тесты на создание заказа")
public class OrderCreationTest {
    static OrderClient orderClient = new OrderClient();
    OrderData order;
    int trackNumber;

    static Stream<Arguments> correctOrderDataForColors() {
        return Stream.of(
                arguments(Arrays.asList("BLACK", "GRAY")),
                arguments(Arrays.asList("BLACK")),
                arguments(Arrays.asList("GRAY")),
                arguments(new ArrayList<String>())
        );
    }

    static Stream<Arguments> incorrectOrderDataForColors() {
        return Stream.of(
                arguments(Arrays.asList("BLACK", "GRAY", "PURPLE")),
                arguments(Arrays.asList("BLA")),
                arguments(Arrays.asList("GRA"))
        );
    }

    @BeforeEach
    @Step("Создание объекта тестового заказа")
    public void setUpOrder() {
        order = new OrderData("Имя", "Фамилия", "Адрес такой-то",
                "5", "+79099099999", 2, "2023-06-06", "Тестируем");
    }


    @ParameterizedTest(name="Создание заказа с корректными вариациями параметра Цвет - {color}")
    @MethodSource("correctOrderDataForColors")
    @DisplayName("Создание заказа с вариациями по цвету")
    @Description("Проверяется, что возможно создать заказ с указанием одного или двух цветов или не указывать цвет вообще")
    public void successfulOrderCreationForColorsVariations(List<String> color) {
        order.setColors(color);
        ValidatableResponse response = orderClient.createOrder(order);
        trackNumber = response.extract().path("track");

        Allure.step("Проверка корректности данных в ответе по созданию заказа", () -> {
            assertAll("Приходит правильный статус-код и трек-номер в виде целого числа",
                    () -> assertEquals(CREATED.getCode(), response.extract().statusCode()),
                    () -> assertThat(trackNumber, is(instanceOf(Integer.class)))
            );
        });
    }

    @ParameterizedTest(name="Создание заказа с некорректными данными по цвету")
    @MethodSource("incorrectOrderDataForColors")
    @DisplayName("Создание заказа с не входящими в документацию вариациями по цвету")
    public void unsuccessfulOrderCreationWithWrongColor(List<String> color) {
        order.setColors(color);
        ValidatableResponse response = orderClient.createOrder(order);

        Allure.step("Проверка статус-кода с ошибкой", () -> {
            assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode());
        });
    }

    @AfterEach
    @Step("Отмена созданного тестового заказа")
    void tearDown() {
        orderClient.cancelOrder(trackNumber);
    }
}

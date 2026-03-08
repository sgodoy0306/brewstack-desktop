package com.brewstack.desktop.api;

import com.brewstack.desktop.api.model.Recipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BrewApiClientTest {

    @Test
    void testGetMenuShouldReturnRecipes() throws Exception {
        BrewApiClient client = new BrewApiClient();

        List<Recipe> menu = client.getMenu();

        assertNotNull(menu, "Menu list should not be null");
        assertFalse(menu.isEmpty(), "Menu list should not be empty");

        System.out.println("First recipe: " + menu.get(0).getName());
    }
}

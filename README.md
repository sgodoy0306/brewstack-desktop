# BrewStack Desktop

A JavaFX 17 point-of-sale application for coffee shops. Works alongside the [BrewStack API](https://github.com/sgodoy0306/coffee-management-api) backend.

---

## What it does

BrewStack Desktop is the front-of-house screen baristas use to take orders, manage stock, and track their progress. It talks to the Spring Boot REST API running locally.

**Main features:**

- **Recipe grid** — photo cards for every drink, greyed out automatically when stock runs low
- **Order builder** — add items, adjust quantities with the − button, see a live total
- **Complete Order** — sends the order to the backend, deducts stock, awards XP to the active barista
- **Barista selector** — choose who is working; each barista has their own XP and level
- **XP & leveling** — XP bar and level badge update after every order in the header panel
- **Stock management** — view all ingredient levels, restock with a custom amount
- **Daily history** — today's revenue and order count; full historical view
- **Add Recipe** — create new menu items with name, price, image URL, and ingredient requirements
- **Add Barista** — register new baristas from the barista selection screen

---

## System overview

```
┌─────────────────────────┐        HTTP / JSON        ┌──────────────────────────┐
│   brewstack-desktop     │  ──────────────────────►  │  coffee-management-api   │
│   JavaFX 17 desktop app │  ◄──────────────────────  │  Spring Boot 3.2 + PG    │
└─────────────────────────┘       localhost:8181       └──────────────────────────┘
```

The desktop app has no local database — all state lives in the API.

---

## Requirements

- Java 17+
- Maven 3.8+
- The [BrewStack API](https://github.com/sgodoy0306/coffee-management-api) running on `localhost:8181`

---

## Running

### 1. Start the backend first

Follow the instructions in the API repo. In short:

```bash
cd coffee-management-api
make db    # starts PostgreSQL in Docker
make run   # starts the Spring Boot server on :8181
```

### 2. Run the desktop app

```bash
cd brewstack-desktop
mvn javafx:run
```

---

## Screens

### Barista Selection
Shown on launch. Lists every barista from the API with their current level and XP. Click a card to start the session as that barista. Use the **+ Add Barista** button to register a new barista.

### POS — Main Screen
The main working view. Left/center area shows drink cards (`RecipeCard`) with their photo, name, and price. Cards load images from the recipe's `imageUrl` and display a greyed-out overlay when stock is insufficient. The right panel is the current order. The header shows the active barista's XP bar and current level.

- Click a card to add it to the order
- Use **−** to reduce quantity or remove an item
- Cards are disabled and dimmed when stock is insufficient
- Press **Complete Order** to submit — stock is deducted and XP is awarded
- Use **+ Add Recipe** to create a new menu item

### Add Recipe
Form to create a new recipe: name, price, image URL, and one or more ingredient requirements (ingredient name + quantity). Calls `POST /recipes` on the API.

### Add Barista
Form to register a new barista by name. Accessible from the Barista Selection screen. Calls `POST /baristas` on the API.

### Stock Management
Accessed via the **Stock** button in the header. Shows every ingredient with its current level and minimum threshold. Enter an amount and press **+ Restock** to add stock live.

### Daily History
Accessed via the **Daily History** button. Shows today's total revenue and order count, plus a table of all historical days.

---

## Project structure

```
src/main/
├── java/com/brewstack/desktop/
│   ├── App.java                      — entry point
│   ├── AppState.java                 — holds the active barista session
│   ├── MainViewController.java       — POS screen controller
│   ├── BaristaSelectionController.java
│   ├── AddBaristaController.java     — new barista form
│   ├── AddRecipeController.java      — new recipe form (name, price, image, ingredients)
│   ├── StockViewController.java
│   ├── DailyHistoryController.java
│   ├── FullHistoryController.java
│   ├── RecipeCard.java               — custom card with image + out-of-stock overlay
│   ├── OrderItemCell.java            — custom list cell with − button
│   ├── StockCell.java                — custom list cell with restock input
│   ├── Barista.java                  — model + XP/level helpers
│   ├── Recipe.java                   — model + isInStock() check + imageUrl
│   ├── RecipeIngredient.java
│   ├── OrderItem.java
│   ├── StockItem.java
│   ├── DailyBalance.java
│   └── api/
│       ├── BrewApiClient.java        — all HTTP calls (orders, recipes, baristas, ingredients)
│       └── model/
│           ├── CreateRecipeRequest.java
│           ├── IngredientDTO.java
│           ├── IngredientRequest.java
│           └── OrderSummaryDTO.java
├── resources/com/brewstack/desktop/
│   ├── MainView.fxml
│   ├── BaristaSelection.fxml
│   ├── AddBaristaView.fxml
│   ├── AddRecipeView.fxml
│   ├── StockView.fxml
│   ├── DailyHistoryView.fxml
│   ├── FullHistoryView.fxml
│   └── images/                       — drink photos (matched by recipe name)
└── module-info.java
```

---

## XP & leveling formula

Matches the backend exactly:

```
level     = floor(sqrt(totalXp / 100)) + 1
xpAtStart = (level - 1)² × 100
xpNeeded  = (2 × level - 1) × 100
```

---

## Tech stack

| Layer      | Technology                     |
|------------|--------------------------------|
| Language   | Java 17                        |
| UI         | JavaFX 21.0.2 (JPMS)           |
| HTTP       | java.net.http (HttpClient)     |
| JSON       | Jackson 2.17                   |
| Build      | Maven + JavaFX Maven Plugin    |
| Backend    | Spring Boot 3.2 (separate repo)|

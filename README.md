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
- **XP & leveling** — XP bar updates after every order; a level-up animation plays on promotion
- **Stock management** — view all ingredient levels, restock with a custom amount
- **Daily history** — today's revenue and order count; full historical view

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
Shown on launch. Lists every barista from the API with their current level and XP. Click a card to start the session as that barista.

### POS — Main Screen
The main working view. Left/center area shows drink cards with their photo, name, and price. The right panel is the current order.

- Click a card to add it to the order
- Use **−** to reduce quantity or remove an item
- Cards are disabled and dimmed when stock is insufficient
- Press **Complete Order** to submit — stock is deducted and XP is awarded

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
│   ├── StockViewController.java
│   ├── DailyHistoryController.java
│   ├── FullHistoryController.java
│   ├── OrderItemCell.java            — custom list cell with − button
│   ├── StockCell.java                — custom list cell with restock input
│   ├── Barista.java                  — model + XP/level helpers
│   ├── Recipe.java                   — model + isInStock() check
│   ├── RecipeIngredient.java
│   ├── OrderItem.java
│   ├── StockItem.java
│   └── DailyBalance.java
├── resources/com/brewstack/desktop/
│   ├── MainView.fxml
│   ├── BaristaSelection.fxml
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
| UI         | JavaFX 17 (JPMS)               |
| HTTP       | java.net.http (HttpClient)     |
| JSON       | Jackson 2.17                   |
| Build      | Maven + JavaFX Maven Plugin    |
| Backend    | Spring Boot 3.2 (separate repo)|

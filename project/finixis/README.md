# Finixis — UI-Only MVP Prototype (Customer Demo)

A clickable, visually complete JavaFX prototype for customer sign-off. **No real
database, no login/auth, no business rules** — every screen is populated with
realistic mock data so the customer can react to layout, flow, and both themes
before real functionality is built.

---

## What's mocked vs. real

| Aspect | Status |
|---|---|
| Database / persistence | **Mocked** — in-memory `MockDataService` creates realistic sample data at startup. No real DB. |
| Login / authentication | **Removed for this MVP** — app launches straight into the Home page, no sign-in step. |
| Role-based access | **Removed for this MVP** — every action is available to everyone; no permission matrix, no role switcher. |
| Save / Delete / Add Debit / etc. | **Live in-memory** — actions update the in-memory mock data so the UI reacts immediately (e.g., adding a debit shows up in the customer's transaction history). |
| Record Payment / Add Debit dialogs | **Fully functional** — item dropdown from mock inventory, quantity, auto-calculated totals; confirms update in-memory data. |
| Generate Report / Create Invoice / Export | **Real files** — clicking these buttons generates genuine, openable `.pdf` and `.xlsx` files (saved to `~/Downloads/Finixis/`) and adds them to the Reports/Summary page for download. |
| Light / Dark theme toggle | **Fully working** — toggle on the Account/Profile page. Every screen and every dialog applies the correct theme. |
| Reports chart | **Real JavaFX BarChart** populated with mock monthly numbers. |

---

## Tech stack (identical to production target — code is reusable later)

- Java 21, JavaFX 21, FXML for layouts
- Maven build (`mvn clean javafx:run`)
- Simple MVVM structure (View/Controller + lightweight `MockDataService`)
- Ikonli (FontAwesome5 pack) for icons
- JavaFX Charts for the Reports/Summary page

---

## How to run

```bash
mvn clean javafx:run
```

Requires Java 21 on `JAVA_HOME`. The app launches directly into the Home / Welcome
page — there is no login step.

---

## Pages (all navigable, all populated with mock data)

1. **Home / Welcome** — launch screen, dashboard stat chips (pending credits/debits,
   low-stock count, customer count), quick action buttons, profile menu (Account,
   Sign Out — Sign Out shows a no-op confirmation).

2. **Accounts / Customers** — search bar (client-side filter), ~12 mock customers
   listed alphabetically, Open button navigates to that customer's detail page.

3. **Dedicated Customer Page** — prominent summary card (avatar/initials, contact,
   customer-since, large color-coded balance: green = we owe them, red = they owe
   us), quick-glance stat chips, **Add Debit** and **Record Payment** action
   buttons, Delete Customer (with confirmation dialog, separated from primary
   actions), and a scannable transaction history section with status labels
   (To Receive / To Pay / All Cleared).

   - **Record Payment dialog** — item dropdown from mock inventory, quantity per
     item, "Add another item" row button, read-only auto-calculated Total/Remaining,
     editable Paid Amount. Confirm creates a Credit entry and updates the customer's
     balance in memory.
   - **Add Debit dialog** — pre-filled customer name (editable), numeric amount.
     Confirm adds a Debit entry and updates the customer's balance in memory.

4. **Transaction History** — chronological list grouped by date, status labels and
   colors (To Receive / To Pay / All Cleared), date-range filter, row click
   navigates to that customer's detail page. **Generate Report**, **Create Invoice**,
   and **Export** buttons produce real downloadable PDF and Excel files that appear
   on the Reports/Summary page.

5. **Credit** — mock pending credits list, search/filter/sort, Mark as Settled
   confirmation, Add New Credit (guides user to Record Payment flow).

6. **Inventory** — ~12 mock items with low-stock badges on a few, Add New Item
   dialog, Edit/Delete icons, Stock In / Stock Out dialog.

7. **Account / Profile** — read-only user info, **light/dark theme toggle**, editable
   phone/email (updates in-memory only).

8. **Reports / Summary** — date range selector, JavaFX bar chart with mock monthly
   totals, and a **Generated Files list** where every report/invoice/export produced
   from the Transaction History page appears — with name, type (PDF/Excel), and
   timestamp — and can be downloaded (opens the actual file on disk).

---

## Themes

Both themes use CSS variables (semantic color tokens) defined in:

- `src/main/resources/css/light-theme.css` — base component styles + light palette
- `src/main/resources/css/dark-theme.css` — overrides the CSS variables with a dark palette

A `ThemeManager` singleton registers every `Scene` (main window and each dialog)
and re-applies the correct stylesheet pair to all registered scenes when the toggle
is flipped. This ensures dialogs and popups are correctly themed regardless of when
they were opened.

Toggle is on the **Account / Profile** page.

---

## Project structure

```
src/main/java/com/finixis/
  App.java                       — entry point, launches to Home
  model/                         — POJOs: Customer, Credit, Transaction,
                                   InventoryItem, Invoice, GeneratedFile, User
  viewmodel/
    MockDataService.java          — realistic in-memory sample data (customers,
                                   inventory, transactions, credits, generated files)
    FileGenerationService.java    — generates real PDF and .xlsx files from mock data
    ThemeManager.java             — registers scenes, applies light/dark stylesheets
    UiUtil.java                   — money formatting, date helpers, toast notifications
  controller/
    ShellController.java          — side nav, page host, customer deep-link
    PageController.java           — marker interface for page controllers
    Dialogs.java                  — themed dialogs: Add Debit, Record Payment,
                                   Add Item, Stock In/Out, Mark Settled, Sign Out
    HomeController.java           — Home/Welcome page
    AccountsController.java       — Customers list page
    CustomerDetailController.java — Dedicated customer page
    TransactionsController.java   — Transaction History page (with file generation)
    CreditController.java         — Credit page
    InventoryController.java      — Inventory page
    AccountController.java        — Account/Profile page (includes theme toggle)
    ReportsController.java        — Reports/Summary page (chart + generated files)
src/main/resources/
  fxml/                           — one .fxml per screen
  css/
    light-theme.css               — full component styles + light palette variables
    dark-theme.css                — dark palette variable overrides only
```

---

## Generated files location

PDF and Excel files are saved to `<user home>/Downloads/Finixis/`. If the folder
doesn't exist it's created automatically. The files remain on disk after the app
closes and can be re-downloaded from the Reports/Summary page during the session.

---

## Next steps (not part of this MVP)

Once the customer approves this UI, subsequent steps can layer in:

- Real persistence (e.g. PostgreSQL/Supabase) behind the existing `MockDataService` interface
- Real authentication and session management
- Real business logic for invoice/report generation
- Role-based permissions and the User Management page

The screens themselves are designed to not need rebuilding — only the data layer
and auth layer need to be added beneath them.

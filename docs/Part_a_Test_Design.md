# Assignment 2 - Part (a): Use Cases, Quality Goals and Test Cases
**Website under test:** https://www.bata.com.pk/ (Shopify storefront)  
**Quality goal for every use case:** 90 % success at 95 % confidence  
**Tools:** Java 17, Selenium WebDriver 4, TestNG, Maven  

---
## Task 1 - Use cases and user stories

| ID | Use case | User story |
|---|---|---|
| UC1 | User registration | As a first-time visitor, I want to create an account with my name, email and password, so that I can check out faster and see my orders. |
| UC2 | Order placement 1 (logged in) | As a registered customer, I want to log in, add a shoe to my cart and proceed through checkout up to the payment step, so that my details are pre-filled and my order is ready to pay. |
| UC3 | Order placement 2 (guest) | As a visitor without an account, I want to add a product to my cart and enter my delivery details as a guest, so that I can order without registering. |
| UC4 | Shopping cart | As a shopper, I want to open my cart and see every item with its name, quantity and price, so that I can review my order before paying. |
| UC5 | Continue shopping | As a shopper who just added an item, I want to click "Continue shopping" and return to the product listing with my cart kept, so that I can keep browsing. |

Common quality goal: **p0 = 90 %, confidence 95 %** -> n = 29 runs with 0 failures, 46 with 1 failure, 61 with 2 failures.

Acceptance-criteria and "stop before payment" rule: UC2 and UC3 end when the checkout page shows the payment section with all contact/shipping fields accepted. **The Pay / Complete-order button is never pressed** (the page object has no method for it).

---
## Task 2 - Equivalence classes and test cases

### UC1 Registration (`/account/register`)
| Input | Valid class (V) | Invalid classes (I) |
|---|---|---|
| First / last name | V1: alphabetic, 1-50 chars | - (optional on most Shopify themes; confirm in pilot) |
| Email | V2: well-formed, never used before | I1: missing `@`; I2: empty; I3: double `@@`; I4: contains space; I5: already registered |
| Password | V3: 8-16 chars, mixed letters/digits/symbol | I6: empty; I7: too short (< 5 chars) |

**TC01 - Registration with valid data** (valid)
- Preconditions: site reachable; email unused.
- Steps: open `/account/register` -> enter name, unique email, password (different generated values each run) -> submit -> open `/account`.
- Expected: account created, user logged in (logout link visible).

**TC02 - Registration with invalid data** (invalid)
- Preconditions: as above. Representatives I1-I7 rotate across runs.
- Steps: open `/account/register` -> enter ONE invalid representative (other fields valid) -> submit -> open `/account`.
- Expected: no account is created and the user is not logged in.

### UC2 Login + order (stop before payment)
| Input | Valid class | Invalid classes |
|---|---|---|
| Email | V1: registered disposable account | I1: unregistered; I2: malformed (`@@`); I3: empty |
| Password | V2: correct password | I4: wrong password; I5: empty |
| Product / size | V3: in-stock product, available size (rotated every run) | - |
| Shipping details | V4: valid name, address, city, postcode, 03xx phone (random each run) | - |

**TC03 - Login and place order (valid)**
- Preconditions: disposable account exists; product in stock.
- Steps: login -> open product -> choose size -> Add to cart -> `/cart` -> Checkout -> fill shipping details -> stop.
- Expected: logged in; item in cart; checkout accepts all fields with no inline error; payment section visible; no order placed.

**TC04 - Login with invalid credentials (invalid)**
- Steps: open `/account/login` -> enter invalid representative (I1-I5, rotating) -> submit -> open `/account`.
- Expected: login rejected, user not authenticated, no order flow possible.

### UC3 Guest order (stop before payment)
| Input | Valid class | Invalid classes |
|---|---|---|
| Email | V1: well-formed | I1: `name@` ; I2: no `@`; I3: empty |
| First name | V2: alphabetic | I4: empty |
| Address | V3: non-empty street address | I5: empty |
| Phone | V4: 11-digit `03xx xxxxxxx` | I6: letters; I7: too short |

**TC05 - Guest checkout with valid data (valid)**
- Steps: open product -> choose size -> Add to cart -> `/cart` -> Checkout (no login) -> fill email + shipping details -> stop.
- Expected: all fields accepted, no inline error, payment section visible, no order placed.

**TC06 - Guest checkout with invalid data (invalid)**
- Steps: as TC05, then overwrite one field with an invalid representative (I1-I7, rotating) and leave the field.
- Expected: an inline validation error is shown for that field.

### UC4 Shopping cart extraction
| Input | Valid class | Invalid classes |
|---|---|---|
| Cart content | V1: 1, 2 or 3 distinct in-stock items (varies each run) | I1: brand-new empty cart; I2: item added then removed |

**TC07 - Extract cart with items (valid)**
- Steps: add k items -> open `/cart` -> read every line (title, quantity, price) -> compare with the store's `/cart.js` data.
- Expected: number of extracted lines equals the cart's line count; every line has a title.

**TC08 - Extract empty cart (invalid)**
- Steps: (I1) open `/cart` on a new session, or (I2) add an item, remove it, open `/cart`.
- Expected: zero lines extracted and the "Your cart is empty" message is shown.

### UC5 Continue shopping
| Input | Valid class | Invalid classes |
|---|---|---|
| Quantity | V1: 1 | I1: 0; I2: -1; I3: 1.5; I4: empty |
| Product / size | V2: in-stock product, rotating size | - |

**TC09 - Continue shopping after adding an item (valid)**
- Steps: add item -> `/cart` -> click "Continue shopping".
- Expected: browser lands on a `/collections/...` page and the cart still contains the item.

**TC10 - Continue shopping after an invalid add (invalid)**
- Steps: open product -> set quantity to an invalid representative (I1-I4, rotating) -> Add to cart -> `/cart` -> click "Continue shopping".
- Expected: nothing is added to the cart (count stays 0) and "Continue shopping" still leads to a collection page.

---
## Task 3 - Execution and statistical decision rule
Each of the 10 test cases is executed with a **different generated value on every run** (unique email, random names/addresses/phones, rotating product, size, cart size and invalid class), with a **random 3-10 s pause** between runs and a **fresh browser** per run.

For p0 = 0.9 and 95 % confidence: 0.9^29 = 0.047 <= 0.05, P(X<=1 fail in 46) = 0.048, P(X<=2 fail in 61) = 0.049.

| After | Failures | Decision |
|---|---|---|
| 29 runs | 0 | ACCEPT |
| 29 runs | >= 1 | run up to 46 |
| 46 runs | <= 1 | ACCEPT |
| 46 runs | 2 | run up to 61 |
| 61 runs | <= 2 | ACCEPT, otherwise REJECT |
| any time | 3 | REJECT |

A use case is **accepted only if both its valid and invalid test cases are accepted.** For invalid test cases a *pass* means the site correctly rejected the bad input.

## Ethics and safety checklist (from the assignment)
- [ ] Read https://www.bata.com.pk/pages/terms-use and https://www.bata.com.pk/policies/terms-of-service before running.
- [ ] 3-10 s random delay between runs (built in).
- [ ] Disposable accounts and fake data only; no real personal or payment data.
- [ ] Never press the Pay / Complete-order button (not implemented).
- [ ] If a CAPTCHA or rate-limit page appears, stop and tell your instructor - do not bypass it.

## Part (b) report outline
1. Introduction, site, group members, tools
2. Use cases, user stories, quality goals (Task 1 above)
3. Equivalence classes and the 10 test cases (Task 2 above)
4. Execution design: data generation, stopping rule, environment (browser, date, network)
5. Results: paste `results/summary.md`; insert `results/plots/pass_fail_all.png`, each `TCxx.png` and `pass_rate_ci.png`
6. Per-use-case accept/reject decision with the confidence interval
7. Defects and failure analysis (use the `detail` column in `results.csv`)
8. Threats to validity (live site changes, stock, rate limiting) and conclusion

# Bata.com.pk - Selenium statistical QA (Assignment 2)

## Setup
1. Install Java 17+, Maven, Chrome. `pip install matplotlib scipy` for the report plots.
2. **Read the site's terms of use first.** Look at `/account/login` and `/account/register` in your browser:
   * If they show normal email + password forms -> the code works as is.
   * If login is email-code / "passwordless" (Shopify new customer accounts) or a CAPTCHA appears -> tell me; the login and
     registration flows need a different design. Do not try to bypass a CAPTCHA.
3. **Verify locators once** (right-click > Inspect) on: register form, login form, a product page (size options, Add to cart),
   the cart rows, and the checkout fields. They are written with fallbacks in `pages/*.java`; edit the `By` constants if a pilot run fails.
4. Register ONE disposable account by hand (e.g. with a mailinator address) for the login use case.
5. Check the products in `Config.PRODUCTS` are in stock (or pass `-Dproducts=handle1,handle2,...`).

## Run
```
# quick pilot of a single test (do 1-2 of these first, headed so you can watch)
mvn test -Dtest=BataUseCaseTests#tc05_guestOrder_valid -Dlogin.email=you@mailinator.com -Dlogin.password='...'

# full suite (10 tests x 29-61 runs; several hours - run it in sessions, results are appended to CSV)
mvn test -Dlogin.email=you@mailinator.com -Dlogin.password='...' -Dheadless=true
```
Other settings: `-Demail.domain=`, `-Dmin.delay=3 -Dmax.delay=10`, `-Dresults.dir=results`.

## Report
```
python analysis/analyze.py results/results.csv
```
Produces `results/summary.md|csv` and the plots in `results/plots/`. Combine with `docs/Part_a_Test_Design.md` for the submission.

## Notes
* A run that errors out (timeout, element not found) counts as a FAILURE - that is deliberate and conservative. Look at the `detail`
  column to separate real site defects from broken locators, fix the locators, and re-run that test from scratch (delete its rows).
* The same shared sample is not reused: delete `results/` before the final full run so pilot runs do not pollute the statistics.

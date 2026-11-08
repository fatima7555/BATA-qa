| Test | Use case | Type | Runs | Pass | Fail | Pass rate | 95% CI (Clopper-Pearson) | 95% lower bound | Decision |
|---|---|---|---|---|---|---|---|---|---|
| TC01 | UC1 Registration | valid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC02 | UC1 Registration | invalid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC03 | UC2 Login + order | valid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC04 | UC2 Login + order | invalid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC05 | UC3 Guest order | valid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC06 | UC3 Guest order | invalid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC07 | UC4 Cart extraction | valid | 31 | 29 | 2 | 93.5% | [78.6%, 99.2%] | 81.1% | **INCOMPLETE** |
| TC08 | UC4 Cart extraction | invalid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC09 | UC5 Continue shopping | valid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |
| TC10 | UC5 Continue shopping | invalid | 29 | 29 | 0 | 100.0% | [88.1%, 100.0%] | 90.2% | **ACCEPT** |

| Use case | Verdict (valid AND invalid test accepted) |
|---|---|
| UC1 Registration | **ACCEPT** |
| UC2 Login + order | **ACCEPT** |
| UC3 Guest order | **ACCEPT** |
| UC4 Cart extraction | **INCOMPLETE** |
| UC5 Continue shopping | **ACCEPT** |

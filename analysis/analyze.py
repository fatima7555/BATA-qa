#!/usr/bin/env python3
"""
Part (b) analysis: reads results/results.csv written by the Selenium run and produces
  - results/summary.csv / summary.md   (n, passes, failures, pass rate, 95% CI, decision per test + per use case)
  - results/plots/pass_fail_all.png    (pass/fail count of every test)
  - results/plots/TCxx.png             (one plot per test)
  - results/plots/pass_rate_ci.png     (pass rate with 95% CI vs the 90% goal)

Usage:  pip install matplotlib scipy
        python analysis/analyze.py [results/results.csv]
"""
import csv
import os
import sys
from collections import OrderedDict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from scipy.stats import beta

P0, ALPHA = 0.90, 0.05
CHECKPOINTS = [(29, 0), (46, 1), (61, 2)]      # (runs, failures allowed)


def cp_interval(k, n, alpha=ALPHA):
    """Two-sided Clopper-Pearson (exact binomial) confidence interval for the pass rate."""
    lo = 0.0 if k == 0 else beta.ppf(alpha / 2, k, n - k + 1)
    hi = 1.0 if k == n else beta.ppf(1 - alpha / 2, k + 1, n - k)
    return lo, hi


def cp_lower_one_sided(k, n, alpha=ALPHA):
    """One-sided 95% lower confidence bound: 'we are 95% sure the true pass rate is at least this'."""
    return 0.0 if k == 0 else beta.ppf(alpha, k, n - k + 1)


def decide(n, fails):
    if fails > CHECKPOINTS[-1][1]:
        return "REJECT"
    for runs, allowed in CHECKPOINTS:
        if n == runs and fails <= allowed:
            return "ACCEPT"
    return "INCOMPLETE" if n < CHECKPOINTS[-1][0] else "REJECT"


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    latest_only = "--latest-session" in sys.argv
    path = args[0] if args else "results/results.csv"
    out_dir = os.path.dirname(path) or "."
    plot_dir = os.path.join(out_dir, "plots")
    os.makedirs(plot_dir, exist_ok=True)

    tests = OrderedDict()
    with open(path, newline="", encoding="utf-8") as f:
        all_rows = list(csv.DictReader(f))
    if latest_only:
        # keep, per test, only the rows from its LAST "run 1" onward (= the most recent complete session);
        # earlier interrupted sessions stay in results.csv but are not counted. Disclose this in the report.
        start = {}
        for i, r in enumerate(all_rows):
            if r["run"] == "1":
                start[r["test_id"]] = i
        all_rows = [r for i, r in enumerate(all_rows) if i >= start.get(r["test_id"], 0)]
        print("[analyze] --latest-session: counting only the most recent session of each test")
    for r in all_rows:
        t = tests.setdefault(r["test_id"], {"use_case": r["use_case"], "kind": r["kind"], "pass": 0, "fail": 0})
        t["pass" if r["result"] == "PASS" else "fail"] += 1

    rows = []
    for tid, t in sorted(tests.items()):
        n = t["pass"] + t["fail"]
        lo, hi = cp_interval(t["pass"], n)
        rows.append({
            "test_id": tid, "use_case": t["use_case"], "kind": t["kind"], "runs": n,
            "passes": t["pass"], "failures": t["fail"], "pass_rate": t["pass"] / n,
            "ci_low": lo, "ci_high": hi, "lower_bound_95_one_sided": cp_lower_one_sided(t["pass"], n),
            "decision": decide(n, t["fail"]),
        })

    # use-case verdict = both its test cases must be accepted
    verdict = OrderedDict()
    for r in rows:
        v = verdict.setdefault(r["use_case"], [])
        v.append(r["decision"])
    uc_decision = {u: ("ACCEPT" if all(d == "ACCEPT" for d in ds) else
                       "INCOMPLETE" if any(d == "INCOMPLETE" for d in ds) and "REJECT" not in ds else "REJECT")
                   for u, ds in verdict.items()}

    with open(os.path.join(out_dir, "summary.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        for r in rows:
            w.writerow({k: (f"{v:.4f}" if isinstance(v, float) else v) for k, v in r.items()})

    with open(os.path.join(out_dir, "summary.md"), "w", encoding="utf-8") as f:
        f.write("| Test | Use case | Type | Runs | Pass | Fail | Pass rate | 95% CI (Clopper-Pearson) | 95% lower bound | Decision |\n")
        f.write("|---|---|---|---|---|---|---|---|---|---|\n")
        for r in rows:
            f.write(f"| {r['test_id']} | {r['use_case']} | {r['kind']} | {r['runs']} | {r['passes']} | {r['failures']} | "
                    f"{r['pass_rate']:.1%} | [{r['ci_low']:.1%}, {r['ci_high']:.1%}] | {r['lower_bound_95_one_sided']:.1%} | **{r['decision']}** |\n")
        f.write("\n| Use case | Verdict (valid AND invalid test accepted) |\n|---|---|\n")
        for u, d in uc_decision.items():
            f.write(f"| {u} | **{d}** |\n")

    ids = [r["test_id"] for r in rows]
    # combined pass/fail plot
    fig, ax = plt.subplots(figsize=(11, 5))
    x = range(len(ids))
    ax.bar([i - 0.2 for i in x], [r["passes"] for r in rows], 0.4, label="Pass", color="#2e7d32")
    ax.bar([i + 0.2 for i in x], [r["failures"] for r in rows], 0.4, label="Fail", color="#c62828")
    ax.set_xticks(list(x))
    ax.set_xticklabels([f"{r['test_id']}\n{r['kind']}" for r in rows], fontsize=8)
    ax.set_ylabel("Number of runs")
    ax.set_title("Pass / fail count per test case - bata.com.pk")
    ax.legend()
    for i, r in enumerate(rows):
        ax.text(i - 0.2, r["passes"] + 0.3, r["passes"], ha="center", fontsize=8)
        ax.text(i + 0.2, r["failures"] + 0.3, r["failures"], ha="center", fontsize=8)
    fig.tight_layout()
    fig.savefig(os.path.join(plot_dir, "pass_fail_all.png"), dpi=150)
    plt.close(fig)

    # one plot per test
    for r in rows:
        fig, ax = plt.subplots(figsize=(4, 3.4))
        ax.bar(["Pass", "Fail"], [r["passes"], r["failures"]], color=["#2e7d32", "#c62828"])
        ax.set_title(f"{r['test_id']} ({r['kind']}) - {r['decision']}\n{r['use_case']}", fontsize=9)
        ax.set_ylabel("Runs")
        for i, v in enumerate([r["passes"], r["failures"]]):
            ax.text(i, v + 0.3, str(v), ha="center")
        fig.tight_layout()
        fig.savefig(os.path.join(plot_dir, f"{r['test_id']}.png"), dpi=150)
        plt.close(fig)

    # pass rate + CI plot
    fig, ax = plt.subplots(figsize=(10, 4.5))
    for i, r in enumerate(rows):
        ax.errorbar(i, r["pass_rate"], yerr=[[r["pass_rate"] - r["ci_low"]], [r["ci_high"] - r["pass_rate"]]],
                    fmt="o", capsize=4, color="#1565c0")
    ax.axhline(P0, color="#c62828", linestyle="--", label="Quality goal 90%")
    ax.set_xticks(range(len(ids)))
    ax.set_xticklabels(ids)
    ax.set_ylim(0.5, 1.02)
    ax.set_ylabel("Pass rate (95% CI)")
    ax.set_title("Observed pass rate with 95% confidence interval")
    ax.legend()
    fig.tight_layout()
    fig.savefig(os.path.join(plot_dir, "pass_rate_ci.png"), dpi=150)
    plt.close(fig)

    print(open(os.path.join(out_dir, "summary.md"), encoding="utf-8").read())
    print("Plots written to", plot_dir)


if __name__ == "__main__":
    main()
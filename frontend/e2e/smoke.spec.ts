import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const WCAG = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

test.describe("@smoke esqueleto da aplicação", () => {
  test("a página inicial abre e mostra a API disponível através do nginx", async ({ page }) => {
    await page.goto("/");

    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await expect(page.getByRole("status")).toHaveText("API disponível");
  });

  test("a página inicial não tem violações de acessibilidade", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("status")).toHaveText("API disponível");

    const results = await new AxeBuilder({ page }).withTags(WCAG).analyze();

    expect(results.violations).toEqual([]);
  });

  test("a página inicial não tem deslocamento horizontal", async ({ page }) => {
    await page.goto("/");

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
    expect(overflow).toBe(false);
  });
});

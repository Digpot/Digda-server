/* eslint-disable */
// Convert markdown files to PDF with Mermaid rendering.
// Usage: node md-to-pdf.cjs file1.md [file2.md ...]

const fs = require("fs");
const path = require("path");

const MD_TO_PDF_NM = "C:/Users/chltm/AppData/Roaming/npm/node_modules/md-to-pdf/node_modules";
const { marked } = require(path.join(MD_TO_PDF_NM, "marked"));
const puppeteer = require(path.join(MD_TO_PDF_NM, "puppeteer-core"));

const CHROME = "C:/Program Files/Google/Chrome/Application/chrome.exe";

const CSS = `
  body { font-family: 'Segoe UI', Arial, sans-serif; color: #222; line-height: 1.55; padding: 40px 56px; max-width: 920px; margin: 0 auto; }
  h1, h2, h3, h4 { color: #1a1a1a; margin-top: 1.6em; }
  h1 { border-bottom: 2px solid #ddd; padding-bottom: .3em; }
  h2 { border-bottom: 1px solid #eee; padding-bottom: .25em; }
  code { background: #f5f5f5; padding: 1px 4px; border-radius: 3px; font-family: Consolas, monospace; font-size: 0.9em; }
  pre { background: #f7f7f7; padding: 12px 14px; border-radius: 6px; overflow-x: auto; border: 1px solid #eee; }
  pre code { background: transparent; padding: 0; }
  table { border-collapse: collapse; width: 100%; margin: 1em 0; font-size: 0.9em; page-break-inside: auto; }
  table thead { display: table-header-group; }
  table tr { page-break-inside: avoid; page-break-after: auto; }
  th, td { border: 1px solid #ddd; padding: 6px 10px; text-align: left; vertical-align: top; }
  th { background: #f0f4f8; }
  blockquote { border-left: 4px solid #ddd; color: #555; padding-left: 14px; margin-left: 0; }
  hr { border: 0; border-top: 1px solid #ddd; margin: 2em 0; }
  .mermaid { text-align: center; margin: 1.2em 0; page-break-inside: avoid; }
  @page { margin: 18mm; }
`;

function renderMarkdown(md) {
  // Replace mermaid code fences with <div class="mermaid"> for client-side rendering.
  const tokens = marked.lexer(md);
  const out = [];
  for (const t of tokens) {
    if (t.type === "code" && (t.lang || "").trim().toLowerCase() === "mermaid") {
      out.push(`\n<div class="mermaid">${escapeHtml(t.text)}</div>\n`);
    } else {
      out.push(marked.parser([t]));
    }
  }
  return out.join("");
}

function escapeHtml(s) {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function buildHtml(title, body) {
  return `<!doctype html>
<html lang="ko"><head>
<meta charset="utf-8" />
<title>${escapeHtml(title)}</title>
<style>${CSS}</style>
</head><body>
${body}
<script type="module">
  import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
  mermaid.initialize({ startOnLoad: false, theme: 'default', flowchart: { htmlLabels: true } });
  await mermaid.run({ querySelector: '.mermaid' });
  window.__mermaidDone = true;
</script>
</body></html>`;
}

async function convert(mdPath, browser) {
  const md = fs.readFileSync(mdPath, "utf8");
  const title = path.basename(mdPath, ".md");
  const html = buildHtml(title, renderMarkdown(md));
  const tmpHtml = path.join(path.dirname(mdPath), `.${title}.tmp.html`);
  fs.writeFileSync(tmpHtml, html);

  const page = await browser.newPage();
  await page.goto("file:///" + tmpHtml.replace(/\\/g, "/"), { waitUntil: "networkidle0", timeout: 60000 });
  // Wait for mermaid done (up to 30s)
  await page.waitForFunction(() => {
    const els = document.querySelectorAll(".mermaid");
    return els.length === 0 || window.__mermaidDone === true;
  }, { timeout: 30000 });

  const pdfPath = path.join(path.dirname(mdPath), `${title}.pdf`);
  await page.pdf({
    path: pdfPath,
    format: "A4",
    printBackground: true,
    margin: { top: "18mm", right: "16mm", bottom: "18mm", left: "16mm" }
  });
  await page.close();
  fs.unlinkSync(tmpHtml);
  console.log(`OK ${pdfPath}`);
}

(async () => {
  const files = process.argv.slice(2);
  if (!files.length) {
    console.error("Usage: node md-to-pdf.cjs file1.md [file2.md ...]");
    process.exit(2);
  }
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: "new",
    args: ["--no-sandbox", "--disable-dev-shm-usage"]
  });
  try {
    for (const f of files) {
      await convert(path.resolve(f), browser);
    }
  } finally {
    await browser.close();
  }
})().catch((e) => {
  console.error(e);
  process.exit(1);
});

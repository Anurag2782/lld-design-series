// Runs every example. Usage:  node src/run_all.ts   (all)   |   node src/run_all.ts strategy   (one)
const topics = [
  "solid/srp", "solid/ocp", "solid/lsp", "solid/isp", "solid/dip",
  "patterns/strategy", "patterns/factory", "patterns/observer", "patterns/builder", "patterns/singleton",
  "patterns/decorator", "patterns/state", "patterns/command", "patterns/chain", "patterns/proxy", "patterns/adapter",
  "patterns/templatemethod", "patterns/facade", "patterns/composite",
  "problems/parkinglot", "problems/splitwise", "problems/bookmyshow", "problems/elevator", "problems/ratelimiter",
  "problems/atm", "problems/library", "problems/chess",
];

const wanted = process.argv[2];
const selected = topics.filter((t) => !wanted || t.endsWith("/" + wanted));
if (selected.length === 0) {
  console.error("No such example: " + wanted);
  process.exit(1);
}
for (const t of selected) {
  console.log(`=== ${t}`);
  const mod = await import(`./${t}.ts`);
  mod.demo();
}
console.log(`\nALL ${selected.length} TYPESCRIPT EXAMPLES PASSED`);

export {};

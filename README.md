# Obfuscation Analyzer

**Static deobfuscator for PHP `eval` chains that extracts *obfuscation signatures* to correlate malicious code.**

Phishing kits and webshells often hide their payload behind layers of `eval`, `base64_decode`, `gzinflate`, `str_rot13`, and similar calls. Obfuscation Analyzer unwinds those layers **without executing the code**. It then keeps what attackers reuse across campaigns: the *structure* of the obfuscation, independent of the payload it hides. Kits that wrap different payloads in the same obfuscation structure fall into the same signature class, which makes the classes usable for clustering and attribution.

This repository is the reference implementation of:

> J. Cassagne, E. Merlo, G.-V. Jourdan, I.-V. Onut. **Following the Obfuscation Trail: Identifying and Exploiting Obfuscation Signatures in Malicious Code.** *Foundations and Practice of Security (FPS 2023)*, LNCS, Springer, 2024, pp. 321–338. [doi:10.1007/978-3-031-57537-2_20](https://doi.org/10.1007/978-3-031-57537-2_20)

It was developed as part of the PhD thesis *Modus Obfuscandi: Analysis of Obfuscation Methods for Correlating Cyber Attacks* (Polytechnique Montréal, 2024).

In the paper, the tool analysed **18,056 `eval` calls** found in a corpus of **31,910 phishing kits**. Where full deobfuscation failed, it still extracted partial signatures. All of these calls reduced to only **569 distinct obfuscation signatures**, and the single most widespread signature appeared in over 400 kits.

> [!NOTE]
> **Research snapshot.** This code is published for reference alongside the paper. The PHP parser front-end (`org.polymtl.codeanalysis.parser`, a JavaCC-generated PHP grammar) and a few helper classes it depends on are **not included**, so the project does not compile on its own.

---

## Example

This is a real obfuscation pattern from the paper's dataset (signature 3). An 86,000+ character Base64 string is stored in a variable and decoded through four layers:

```php
$code = "... large Base64 string (86,000+ characters) ...";
// [...]
eval(str_rot13(gzinflate(str_rot13(base64_decode($code)))));
```

Obfuscation Analyzer:

1. finds the `eval` call;
2. resolves `$code` to its definition using control-flow and data-dependency information (**dataflow resolution**);
3. emulates each decoder in turn, `base64_decode` → `str_rot13` → `gzinflate` → `str_rot13`, to recover the decoded string (**dynamic resolution**, done statically in Java);
4. re-parses the decoded string as PHP (**parsing**). Here it yields one large payload: a password-protected webshell.

The resulting signature records the whole transformation chain, with the parsed payload collapsed into a single `Payload` node. In simplified form (`ArgumentList`/`Argument` nodes omitted; the resolved values are `HexLiteral` nodes):

```
FunctionCall eval
└─ str_rot13 ──DynamicResolution──▶ HexLiteral ──Parse──▶ Payload
   └─ gzinflate ──DynamicResolution──▶ HexLiteral
      └─ str_rot13 ──DynamicResolution──▶ HexLiteral
         └─ base64_decode ──DynamicResolution──▶ HexLiteral
            └─ Variable $code ──DataflowResolution──▶ StringExpression
```

Another kit that applies the same structure to a different payload lands in the same **parametric** class as this one.

---

## How it works

The analysis is entirely static. It works on pre-parsed PHP abstract syntax trees (ASTs) and rewrites each `eval` argument until it reaches a fixed point:

| Transformation | What it does | Edge in the output graph |
|---|---|---|
| **Dynamic resolution** | Emulates a decoder call on a resolved argument, or folds a `.` concatenation of two literals | `DynamicResolution` |
| **Dataflow resolution** | Replaces a variable with its reaching definition, also across included files of the same kit. A variable assigned from a function call is replaced by that call expression | `DataflowResolution` |
| **Parsing** | Re-parses a decoded string as PHP and processes nested `eval` calls recursively | `Parse` |
| **Error** | Marks a step that could not be resolved or an unsupported structure (`DecodeFailed`, `ParseFailed`, `VisitFailed`). Partial signatures are still extracted | — |

**Supported decoders:** `base64_decode`, `base64_encode`, `str_rot13`, `strrev`, `rawurldecode`, `urldecode`, `gzinflate`, `gzdeflate`, `gzcompress`, `gzuncompress`. Decoder names given as strings (e.g. `"base64_decode"($x)`) are also handled. Some argument types are only supported for certain decoders:

- `gz*` and `base64_encode` only accept binary input produced by another decoder or a nested call.
- Variable arguments are resolved for `eval`, `base64_decode`, `str_rot13`, `strrev`, `(raw)urldecode`, and concatenations.

**Recursion limit:** if a decoded payload parses to more than 500 AST nodes, recursion stops and the tool logs `##WEBSHELL DETECTED##`. In the paper's manual analysis, large payloads were typically complete webshells.

**Signatures.** Each extracted `eval` subtree is one signature. It keeps its resolution and parse edges, and the final parsed payload is collapsed into a single `Payload` node. Signatures are grouped into two kinds of class:

- **Identical**: same tree shape, node types *and* node values, including the encoded and decoded strings.
- **Parametric**: same tree shape and node types. Any node value may differ: literals, function names, variable names. This matches the paper's notion of a signature, the obfuscation structure with its literal values abstracted away.

---

## Usage

Entry point: `org.polymtl.codeanalysis.EvalResolver`

```sh
mkdir -p logs out
java -cp <classpath> org.polymtl.codeanalysis.EvalResolver \
     -f filelist.txt -iast ast/ -idd dd/ -o out/
```

| Option | Argument | Required | Description |
|---|---|---|---|
| `-f`, `--filelist` | file | yes | Text file listing the inputs, one per line, in the form `./<kit>/path/file.php` relative to `--in-dir`. The first path component is used as the kit name. Blank lines and lines starting with `#` are ignored. `.ast.json.gz` is appended when needed. |
| `-iast`, `--in-dir` | dir | yes | Directory containing the pre-parsed ASTs (`*.ast.json.gz`; uncompressed `*.ast.json` files must be listed with their full name). Include the trailing `/`. |
| `-idd`, `--in-dir-dd` | dir | no | Directory with control-flow graphs and data-dependency facts (`cfg/<file>.cfg.json`, `cfg/<kit>/`, `dd/kitsDd_<kit>.php.cfg.json`). Without it, `eval` calls that depend on a variable are reported as dataflow errors. |
| `-o`, `--out-dir` | dir | yes | Output directory. Create it beforehand. |
| `-d`, `--debug` | — | no | Verbose logging. |

Log files are written to `./logs/` if that directory exists. Otherwise the tool logs to the console only.

### Inputs

- **ASTs**: one file per PHP source, in a line-oriented "flat JSON" format: a `[` line, then one record per line (`["type",1,"FunctionCall"]`, `["image",1,"eval"]`, `["ast_succ",0,1]`, …), then a `]` line. They are produced by the PHP parser front-end, which is not included here.
- **CFG/DD** (optional): control-flow graphs can be generated from the ASTs with `org.polymtl.codeanalysis.util.FormatForDD`. The data-dependency facts come from a separate def-use analysis.

### Outputs

```
out/
├── full/
│   ├── dot/<file>.astdyn.dot    # Graphviz view of each file's "dynamic AST", with resolution edges
│   └── json/<file>.astdyn.json  # same graph as flat JSON
├── MO/
│   ├── all/<file>.<n>.astdyn.json        # every extracted signature
│   ├── ident/<class>/<n>.astdyn.json     # identical-signature classes
│   └── param/<class>/<n>.astdyn.json     # parametric-signature classes
├── list.txt         # files containing at least one eval
├── list_data.txt    # files whose evals failed on dataflow
├── report.txt       # summary (also printed to stdout)
└── data_class.json  # payload sizes and class sizes, for analysis
```

`MO` stands for *modus operandi*, the shared obfuscation strategy that the signatures capture.

`report.txt` summarises the run: files found, parse failures, files with `eval` (resolved, or failed on dataflow, parsing or another error), total signatures, and the number of identical and parametric classes.

### Other tools

| Class | Purpose |
|---|---|
| `util.FormatForDD` | Builds CFGs from a list of ASTs, in the format used by the data-dependency analysis |
| `util.JSONtoDOT` | Converts an uncompressed JSON graph (parse tree, AST, dynamic AST, CFG) to Graphviz DOT on stdout |
| `util.CFGTranslator` | Converts a single CFG JSON file (`-f`) to the data-dependency format on stdout |

---

## Build requirements

Java 8 or later, with:

- [org.json](https://mvnrepository.com/artifact/org.json/json) `20230227`
- [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) `1.5.0`

These are declared in `src/pom.xml`, which sets no compiler level or source directory. The sources live under `src/codeanalysis/` (package `org.polymtl.codeanalysis`). As noted above, compiling also requires the PHP parser package, which is not part of this repository.

---

## Citation

```bibtex
@inproceedings{Cassagne-2023,
  author    = {Cassagne, Julien and Merlo, Ettore and Jourdan, Guy-Vincent and Onut, Iosif-Viorel},
  title     = {Following the Obfuscation Trail: Identifying and Exploiting Obfuscation Signatures in Malicious Code},
  booktitle = {Foundations and Practice of Security},
  series    = {Lecture Notes in Computer Science},
  pages     = {321--338},
  year      = {2024},
  publisher = {Springer Nature Switzerland},
  address   = {Cham},
  doi       = {10.1007/978-3-031-57537-2_20}
}

@phdthesis{Cassagne-2024-thesis,
  author = {Cassagne, Julien},
  title  = {Modus Obfuscandi: Analysis of Obfuscation Methods for Correlating Cyber Attacks},
  school = {Polytechnique Montr{\'e}al},
  year   = {2024}
}
```

---

## License

Copyright © 2021–2023 the authors and Polytechnique Montréal. **All rights reserved.**

This code is shared for research reference only. Copying, redistribution or reuse requires prior written permission from Polytechnique Montréal or the authors. For permission, contact the author.

# ユーザーストーリー

Coding Agent がコーディングからコーディング後の検証・品質担保のサイクルを自律的して進めることができる


## ユーザーストーリーために実装するもの

- Claude Code でコーディングからコーディング後の検証・品質担保を実行する sub-agent
- sub-agent をオーケストレーションするためのカスタムコマンド


## Claude Code でコーディングからコーディング後の検証・品質担保を実行する sub-agent

1.	Spec & Design Agent（SDA）
＝仕様・設計の決定と更新を担う
2.	Delivery Agent（DA）
＝実装・デプロイ・運用変更を担う
3.	Quality Gate Agent（QGA）
＝レビュー・テスト・コンプライアンスチェックを担う

### 1. 共通の前提（全 Agent に共通する要件）

共通要件
•	共通コンテキストにアクセスできること
•	プロダクトビジョン・ドメインモデル
•	リポジトリ（Read）
•	Issue / PR / チケット
•	ログ・メトリクス・テスト結果
•	自分の責任範囲外のことは“提案まで”で止める
•	例）QGA は「コードを直接修正しない」、SDA は「マージはしない」
•	他 Agent とのインターフェースが明確
•	入力：どんな情報をもらうか
•	出力：どのフォーマットで返すか（コメント・PR・チケット等）
•	次に呼ぶべき Agent は誰か


### 2. Spec & Design Agent（SDA）の要件

役割 / 目的
•	プロダクトや機能の 仕様・設計・非機能要件を言語化し、更新する役
•	「何を・なぜ・どの制約で作るか」を定義する

入力（Input）
•	ビジネス要求・プロダクトゴール
•	既存のドメインモデル・アーキテクチャ図
•	ユーザー行動データ（必要なら）
•	QGA/DA からのフィードバック（例：パフォーマンス問題、運用上の痛み）

出力（Output）
•	構造化された仕様書（PRD / ユーザーストーリー）
•	アーキテクチャ提案（テキスト＋図）
•	非機能要件（SLO, セキュリティ要求, スケーラビリティ等）
•	「受け入れ条件（Acceptance Criteria）」リスト
•	DA/QGA 用のタスクや Issue

機能要件（できること）
•	ビジネス要求からエピック・ストーリーへの分解
•	API 仕様のドラフト（エンドポイント・スキーマ）
•	データモデルの提案（エンティティ・リレーション）
•	競合・類似プロダクトの仕様からの差分整理（オプション）
•	懸念点の洗い出し（リスク・制約）

非機能要件（ふるまい）
•	一貫したフォーマットで仕様を出す（機械可読寄りに）
•	曖昧な要件を残さず、DA / QGA が判断に迷わない粒度で書く
•	自身の過去の仕様と整合性を取りにいく（自己レビュー）

境界（やらないこと）
•	コードを書く・変更する
•	テスト結果をもとに「このリリース OK」を直接判断する
•	本番環境の操作

### 3. Delivery Agent（DA）の要件

役割 / 目的
•	SDA が定義した仕様に基づき、
コード・インフラ・設定を具体的に変更する “実装担当”

入力（Input）
•	SDA が出した仕様・設計
•	既存コードベース
•	QGA からの指摘・改善要求
•	CI/CD パイプライン定義

出力（Output）
•	具体的なコード変更（PR）
•	マイグレーションスクリプト
•	テストコード（ユニット / インテグレーション）
•	インフラ設定変更（IaC の差分）
•	変更概要（Changelog / リリースノートの草案）

機能要件（できること）
•	仕様から実装タスクを分解
•	対象リポジトリを読み解き、変更箇所を特定
•	コード生成・リファクタリング提案
•	ローカルまたは CI 上でのテスト実行（コマンドの生成・誘導）
•	パフォーマンス・リソース観点の初期的な考慮（ただし最終判断はQGA）

非機能要件（ふるまい）
•	仕様と実装の差分を説明できる（「こう解釈したのでこう書いた」）
•	変更範囲をできるだけ局所化する（影響範囲の限定）
•	コードスタイル・規約（SDA が定めたもの）に準拠する

境界（やらないこと）
•	自分で自分の PR をマージしない
•	「テストが不十分だが、まあ大丈夫だろう」と判断しない
•	本番リリースの Go/No-Go 判定は QGA に委ねる


### 4. Quality Gate Agent（QGA）の要件

役割 / 目的
•	DA が行った変更を 品質・安全性・整合性の観点でチェックして、Gate を通すか決める

入力（Input）
•	DA の PR / 差分
•	SDA の仕様・非機能要件
•	自動テスト結果・静的解析結果
•	本番/ステージングのメトリクス・ログ（必要に応じて）

出力（Output）
•	レビューコメント（問題点・提案・質問）
•	テストケース追加の提案 or 自動生成されたテスト
•	「承認 / 差し戻し / 条件付き承認」のステータス
•	障害・バグ発生時の原因分析レポート（Postmortem ドラフト）

機能要件（できること）
•	仕様と実装の乖離を検出
•	規約違反・アンチパターン・セキュリティリスクの指摘
•	テスト結果の評価（カバレッジ・ケースの抜け漏れ確認）
•	性能・リソース・コストの劣化がないかの判断（必要なら）
•	リリース可否の提案（「この状態なら小規模ロールアウトにとどめるべき」など）

非機能要件（ふるまい）
•	一貫した基準で判断する（人によってブレない）
•	過剰品質になりすぎず、リスクとスピードのバランスを見る
•	指摘はなるべく具体的（どの行・どのロジックが、どのルールに反しているか）

境界（やらないこと）
•	自分で本番環境にデプロイをトリガーしない（提案までは可）
•	仕様を勝手に変更しない（変更が必要だと判断したら SDA へ差し戻し）
•	大規模なコード修正を勝手に行わない（必要なら DA へのタスクとして返す）


## sub-agent をオーケストレーションするためのカスタムコマンド

### 1. 基本構造

#### ファイル形式・配置
| 項目 | 内容 |
|------|------|
| 形式 | Markdown（`.md`）+ YAML Frontmatter |
| プロジェクト | `.claude/commands/` |
| ユーザー | `~/.claude/commands/` |

#### Frontmatter（必須/推奨フィールド）

```yaml
---
description: ワークフローの説明（必須：SlashCommand Toolで自動実行するため）
argument-hint: [引数のヒント]
allowed-tools: Task, Read, Edit, Bash(git:*), ...
model: claude-sonnet-4-5-20250929  # オプション
---
```

### 2. Sub-agent 呼び出しの記法

メインエージェントは自動委譲するか、明示的に呼び出せます（「Use the test-runner sub agent…」のように）。

明示的に会話内でエージェントを呼び出せます：
`Use the code-reviewer subagent to check my recent changes`
明示的呼び出しは決定論的であり、予期しない委譲を避けたいプロダクションフローで推奨されます。

#### 呼び出しパターン
```markdown
# 単一エージェント呼び出し
Use the **backend-architect** subagent to design the API structure.

# 複数エージェントの順次呼び出し
Use the **requirements-analyst** to analyze these requirements, 
then have the **system-architect** design the solution.

# 明示的なタスク指定
Use the **security-auditor** subagent on "authentication module".
```

### 3. オーケストレーションの設計原則

#### 明示的なステップ分解が必要

sub-agent の利用を最大化するには、どのステップが sub-agent に委譲されるかの詳細を含む明示的なステップを Claude に提供する必要があります。これはマルチスレッドプログラミングに似ています。ステップをオーケストレーションすればするほど、全体のワークフローは速く完了します。

#### 制約事項

sub-agent は他の sub-agent を生成できません（無限ネストを防ぐため）。

メインの Claude エージェントとは異なり、Claude Code の sub-agent は現在、段階的なプランの生成や実行をサポートしていません。割り当てられたタスクをすぐに実行し始めます。

### 4. カスタムコマンドのテンプレート例

#### 基本的なオーケストレーションコマンド

```markdown
---
description: Feature development with multi-agent orchestration
argument-hint: [feature-description]
allowed-tools: Task, Read, Write, Edit, Bash, Glob, Grep
---

# Feature Development Workflow

Implement the following feature: $ARGUMENTS

## Orchestration Steps

### Phase 1: Planning
Use the **planner** subagent to create a detailed implementation plan.
Output: /docs/plan.md

### Phase 2: Architecture Review  
Use the **architect** subagent to review and validate the plan.
Focus on: API design, database schema, security considerations.

### Phase 3: Implementation
Use the **implementer** subagent to implement based on the approved plan.
Follow the commands listed in /docs/plan.md.

### Phase 4: Testing
Use the **test-automator** subagent to:
- Generate unit tests
- Run test suite
- Fix any failing tests

### Phase 5: Security Audit
Use the **security-auditor** subagent to review for vulnerabilities.

### Phase 6: Final Review
Use the **code-reviewer** subagent for final quality check.

## Completion Criteria
- All tests passing
- Security audit passed
- Code review approved
```

#### 並列処理を意識したコマンド

簡略化されたCLAUDE.mdの例（タスク分割用）:
```
### 並列機能実装ワークフロー
1. **Component:** メインコンポーネントファイルを作成
2. **Styles:** コンポーネントスタイル/CSSを作成
3. **Tests:** テストファイルを作成
4. **Types:** 型定義を作成
5. **Hooks:** カスタムフック/ユーティリティを作成
6. **Integration:** ルーティング、インポート、エクスポートを更新
7. **Remaining:** package.json、ドキュメント、設定ファイルを更新
8. **Review and Validation:** 統合を調整、テストを実行、ビルドを検証、競合をチェック
```

### 5. Sub-agent の定義（参考）

オーケストレーションコマンドが呼び出す sub-agent は `.claude/agents/` に配置:

```markdown
---
name: backend-architect
description: Senior backend architect. PROACTIVELY design API structures and database schemas.
tools: Read, Grep, Glob, Bash
model: opus
---

You are a senior backend architect specializing in API design...
```

より積極的な sub-agent の使用を促すには、description フィールドに「use PROACTIVELY」や「MUST BE USED」などのフレーズを含めます。

### 6. CLAUDE.md との連携

Claude が積極的に subagent を使用しないことがあるため、CLAUDE.md にリマインダーを追加して、これらのスペシャリストに作業を割り当てるよう指示しています:
```
## 👥 SUB-AGENT DELEGATION SYSTEM 👥
** SMART DELEGATION: YOU HAVE 12 SPECIALIZED EMPLOYEES AVAILABLE!**
**⚠️ CRITICAL BEHAVIOR: BE PROACTIVE WITH SUB-AGENTS!**
```

### 7. 要件チェックリスト

| 要件 | 詳細 |
|------|------|
| **description 必須** | SlashCommand Tool で自動実行させるため |
| **明示的な呼び出し構文** | `Use the [agent-name] subagent to [task]` |
| **ステップの順序明示** | Phase 1, 2, 3... または番号付きリスト |
| **各ステップの成果物定義** | 何を出力するか明記 |
| **完了条件の定義** | ワークフロー完了の判断基準 |
| **ネスト不可の考慮** | メインエージェントがすべての sub-agent を直接呼び出す設計 |
| **allowed-tools に Task を含める** | sub-agent 呼び出しに必要 |

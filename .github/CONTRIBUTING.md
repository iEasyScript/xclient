# Contributing to ProjectX

Thanks for your interest in contributing!  
Please read this guide carefully before opening a pull request.

---

## Branching Model

- **Fork → Development → Main**
  - You must **fork this repository** first.  
  - All pull requests must target the **`development`** branch of the upstream ProjectX repository.  
  - **`main`** is protected and updated only by **Mocrosoft**, who merges from `development` into `main`.  

- **Repository scope**
  - This repository accepts **client changes only**.  
  - **No scripts are allowed** here.  

---

## Contribution Flow

```mermaid
flowchart TD
    A[Fork ProjectX Repo] --> B[Clone your fork locally]
    B --> C[Create a branch in your fork]
    C --> D[Commit and push changes]
    D --> E[Open Pull Request to upstream development]
    E --> F[Community Review]
    F --> G[Merged into development]
    G --> H[Only the owner merges development → main]

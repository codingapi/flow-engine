# AGENTS.md

## Build/Test Commands

```bash
# Run all tests
pnpm run test

# Run specific tests by name
pnpm run test:format    # format test
pnpm run test:toNode    # node tests
pnpm run test:toEdge    # edge tests
pnpm run test:toRender  # render tests

# Build library
pnpm run build
pnpm run dev  # watch mode
```

## Code Style Guidelines

### TypeScript
- **Target:** ES2022 with React JSX
- **Strict mode:** Enabled
- **Import style:** ES modules with `.js` extension

### Naming Conventions
- **Classes:** PascalCase (e.g., `WorkflowEdgeConvertor`)
- **Interfaces:** PascalCase with descriptive names
- **Methods:** camelCase (e.g., `loadNextEdges`)
- **Private fields:** `private readonly` preferred
- **Constants:** UPPER_SNAKE_CASE for true constants

### Import Order
1. External libraries (`@rstest/core`, `lodash-es`)
2. Workspace packages (`@flow-engine/*`)
3. Internal aliases (`@/*`)
4. Relative imports (`./utils`)

### Testing with rstest
```typescript
import {describe, expect, test} from "@rstest/core";

describe.sequential('Suite', () => {
    test('case', () => {
        expect(actual).toBe(expected);
    });
});
```

### Error Handling
- Use type-safe error handling
- Validate inputs early with guard clauses
- Log debug info via `console.log` in tests

### Architecture Patterns
- **Convertor Pattern:** Separate conversion logic (e.g., `WorkflowEdgeConvertor`)
- **Builder Utilities:** Use test utilities like `WorkflowUtils` for test setup
- **Filter Nodes:** Define node type filters as readonly arrays

## Documentation

- Rslib: https://rslib.rs/llms.txt
- Rsbuild: https://rsbuild.rs/llms.txt
- Rspack: https://rspack.rs/llms.txt

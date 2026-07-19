ALTER TABLE exception_cases
    ADD COLUMN category VARCHAR(16) NOT NULL DEFAULT 'BUSINESS' AFTER case_no;

CREATE INDEX idx_exception_category_status ON exception_cases (category, status);

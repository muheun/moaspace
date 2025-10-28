-- Users 테이블 생성 (Google OAuth 사용자 저장)
-- Constitution Principle I 준수: 운영 사용자 데이터는 DB에 저장
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 이메일 기반 고속 조회를 위한 유니크 인덱스
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- 샘플 사용자 데이터 (개발/테스트용)
INSERT INTO users (email, name, profile_image_url) VALUES
('test@example.com', '테스트 사용자', 'https://via.placeholder.com/150');

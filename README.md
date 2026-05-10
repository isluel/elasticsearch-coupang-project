# Coupang API Server

강의 실습을 기반으로 구현한 **상품 검색 API 서버**입니다.  
상품 데이터는 MySQL에 저장하고, 검색에 필요한 문서는 Elasticsearch에 함께 저장하여 상품 목록 조회, 상품 등록/삭제, 검색, 자동완성 기능을 제공합니다.

## 프로젝트 개요

이 프로젝트는 쇼핑몰 상품 데이터를 등록하고 검색하는 백엔드 API 서버입니다.  
일반적인 상품 정보는 관계형 데이터베이스인 MySQL에 저장하고, 검색 성능과 검색 품질이 필요한 상품명/설명/카테고리 검색은 Elasticsearch를 활용하도록 구성했습니다.

특히 Elasticsearch의 `nori` 분석기, 동의어 필터, 자동완성 필드를 활용하여 한글 상품명 검색과 검색어 추천 기능을 구현했습니다.

## 주요 기능

- 상품 목록 조회
- 상품 등록
- 상품 삭제
- Elasticsearch 기반 상품 검색
- 상품명 자동완성
- 카테고리 필터 검색
- 가격 범위 필터 검색
- 평점 기반 검색 가중치 적용
- 검색 결과 상품명 하이라이팅

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Web | Spring Web |
| ORM | Spring Data JPA |
| Database | MySQL |
| Search Engine | Elasticsearch |
| Build Tool | Gradle |

## 프로젝트 구조

```text
src/main/java/com/example/coupangapiserver
├── CoupangApiServerApplication.java
└── product
    ├── ProductController.java
    ├── ProductService.java
    ├── ProductRepository.java
    ├── domain
    │   ├── Product.java
    │   └── ProductDocument.java
    ├── dto
    │   └── CreateProductRequestDto.java
    └── repository
        └── ProductDocumentRepository.java

src/main/resources
├── application.yml
└── elasticsearch
    └── product-settings.json
```

## 도메인 구조

### Product

MySQL에 저장되는 상품 엔티티입니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| id | Long | 상품 ID |
| name | String | 상품명 |
| description | String | 상품 설명 |
| price | int | 가격 |
| rating | double | 평점 |
| category | String | 카테고리 |

### ProductDocument

Elasticsearch `products` 인덱스에 저장되는 검색용 문서입니다.

| 필드 | 설명 |
| --- | --- |
| id | 상품 ID |
| name | 상품명 검색 및 자동완성 대상 |
| description | 상품 설명 검색 대상 |
| price | 가격 필터 대상 |
| rating | 평점 가중치 대상 |
| category | 카테고리 검색 및 필터 대상 |

## Elasticsearch 검색 구성

`ProductDocument`는 `products` 인덱스에 저장됩니다.

```java
@Document(indexName = "products")
@Setting(settingPath = "/elasticsearch/product-settings.json")
public class ProductDocument {
    ...
}
```

### 상품명 검색

상품명은 `product_name_analyzer`를 사용합니다.

- `nori_tokenizer` 기반 한글 분석
- 소문자 변환
- 동의어 처리
- `search_as_you_type` 기반 자동완성 지원

### 상품 설명 검색

상품 설명은 `product_description_analyzer`를 사용합니다.

- HTML 태그 제거를 위한 `html_strip` 적용
- `nori_tokenizer` 기반 분석
- 소문자 변환

### 카테고리 검색

카테고리는 텍스트 검색과 정확한 필터 검색을 함께 지원합니다.

- `category`: 검색용 text 필드
- `category.raw`: 정확한 일치 필터용 keyword 필드

## 동의어 설정

`src/main/resources/elasticsearch/product-settings.json`에 상품 검색용 동의어가 정의되어 있습니다.

예시:

```json
"synonyms": [
  "samsung, 삼성",
  "apple, 애플",
  "노트북, 랩탑, 컴퓨터, computer, laptop, notebook",
  "전화기, 휴대폰, 핸드폰, 스마트폰, 휴대전화, phone, smartphone, mobile phone, cell phone",
  "아이폰, iphone",
  "맥북, 맥, macbook, mac"
]
```

이를 통해 사용자가 `삼성`, `samsung`, `노트북`, `laptop` 등 서로 다른 표현으로 검색해도 관련 상품이 검색될 수 있도록 구성했습니다.

## API 명세

### 상품 목록 조회

```http
GET /products?page=1&size=10
```

#### Query Parameter

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| page | 1 | 페이지 번호 |
| size | 10 | 페이지 크기 |

---

### 상품 등록

```http
POST /products
Content-Type: application/json
```

#### Request Body

```json
{
  "name": "삼성 노트북",
  "description": "업무용으로 사용하기 좋은 노트북입니다.",
  "price": 1200000,
  "rating": 4.5,
  "category": "노트북"
}
```

상품 등록 시 MySQL의 `products` 테이블에 상품 데이터가 저장되고, Elasticsearch의 `products` 인덱스에도 검색용 문서가 함께 저장됩니다.

---

### 상품 삭제

```http
DELETE /products/{id}
```

상품 삭제 시 MySQL 데이터와 Elasticsearch 문서를 함께 삭제합니다.

---

### 상품 자동완성

```http
GET /products/suggestions?query=삼
```

#### Query Parameter

| 이름 | 설명 |
| --- | --- |
| query | 자동완성 검색어 |

#### Response 예시

```json
[
  "삼성 노트북",
  "삼성 스마트폰"
]
```

자동완성은 Elasticsearch의 `search_as_you_type` 필드를 사용합니다.

---

### 상품 검색

```http
GET /products/search?query=노트북&category=노트북&minPrice=100000&maxPrice=2000000&page=1&size=5
```

#### Query Parameter

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| query | 필수 | 검색어 |
| category | 선택 | 카테고리 필터 |
| minPrice | 0 | 최소 가격 |
| maxPrice | 1000000000 | 최대 가격 |
| page | 1 | 페이지 번호 |
| size | 5 | 페이지 크기 |

#### 검색 조건

- `name`, `description`, `category` 필드를 대상으로 `multi_match` 검색을 수행합니다.
- 상품명에는 가장 높은 가중치가 적용됩니다.
- 오타 허용을 위해 `fuzziness: AUTO`를 사용합니다.
- 카테고리 필터는 `category.raw`를 사용해 정확히 일치하는 상품만 조회합니다.
- 가격은 `minPrice`와 `maxPrice` 범위로 필터링합니다.
- 평점이 4.0보다 높은 상품은 `should` 조건으로 검색 점수에 가중치를 받을 수 있습니다.
- 검색된 상품명에는 `<b>` 태그를 사용한 하이라이팅이 적용됩니다.

## 실행 환경

이 프로젝트를 실행하려면 다음 서비스가 필요합니다.

- Java 17
- MySQL
- Elasticsearch

현재 설정 기준:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:13306/coupang
    username: root
    password: 1234
  elasticsearch:
    uris: http://localhost:9200
```

## 실행 방법

### 1. MySQL 데이터베이스 생성

```sql
CREATE DATABASE coupang;
```

### 2. Elasticsearch 실행

Elasticsearch는 `localhost:9200`에서 접근 가능해야 합니다.

### 3. 애플리케이션 실행

Windows 기준:

```bash
gradlew.bat bootRun
```

macOS/Linux 기준:

```bash
./gradlew bootRun
```

## 테스트 예시

### 상품 등록

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "삼성 노트북",
    "description": "업무용으로 사용하기 좋은 고성능 노트북입니다.",
    "price": 1200000,
    "rating": 4.5,
    "category": "노트북"
  }'
```

### 상품 검색

```bash
curl "http://localhost:8080/products/search?query=노트북&page=1&size=5"
```

### 자동완성 조회

```bash
curl "http://localhost:8080/products/suggestions?query=삼"
```

## 구현 포인트

### 1. MySQL과 Elasticsearch 이중 저장 구조

상품 등록 시 JPA를 통해 MySQL에 원본 데이터를 저장한 뒤, ElasticsearchRepository를 통해 검색용 문서를 별도로 저장합니다.

```java
Product saveProduct = productRepository.save(product);
productDocumentRepository.save(productDocument);
```

이를 통해 데이터 저장은 RDBMS가 담당하고, 검색 기능은 Elasticsearch가 담당하도록 역할을 분리했습니다.

### 2. 검색 품질 개선

상품 검색에서는 `multi_match`, `fuzziness`, 필드별 가중치, 가격 필터, 카테고리 필터, 평점 조건을 조합했습니다.

```java
Query multiMatchQuery = MultiMatchQuery.of(m -> m
        .query(query)
        .fields("name^3", "description^1", "category^2")
        .fuzziness("AUTO")
)._toQuery();
```

### 3. 한글 검색 대응

Elasticsearch의 `nori_tokenizer`를 사용하여 한국어 상품명과 설명을 검색할 수 있도록 구성했습니다.

### 4. 자동완성 기능

상품명 필드에 `search_as_you_type`을 적용하고, `BoolPrefix` 타입의 `multi_match` 쿼리를 사용하여 입력 중인 검색어에 대한 자동완성 결과를 제공합니다.

## 학습한 내용

- Spring Boot 기반 REST API 구현
- Spring Data JPA를 활용한 MySQL 연동
- Spring Data Elasticsearch를 활용한 검색 문서 저장
- Elasticsearch Analyzer 설정
- 한글 검색을 위한 nori 분석기 사용
- 자동완성 검색 구현
- 필터, 가중치, 하이라이팅을 포함한 Elasticsearch Query 구성

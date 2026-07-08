-- V1__baseline.sql — 마켓온 스키마 베이스라인 (Flyway 도입 시점)
-- 생성: 2026-07-08 · 기준 커밋 dc94fa9 · Hibernate 스키마 스크립트 생성(엔티티 → MySQL DDL)
--
-- ⚠️ 실행된 뒤에는 절대 수정하지 않는다(Flyway checksum 불변). 스키마 변경은 V2, V3... 로 추가.
-- ⚠️ 클라우드(운영) DB에 validate 켜기 전, 실제 운영 스키마와 이 baseline이 일치하는지 반드시 대조할 것
--    (운영 DB는 그동안 ddl-auto:update 누적본이라 미세 차이 가능 — ADR 0003/0004).

    create table categories (
        id bigint not null auto_increment,
        name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table chat_messages (
        chat_room_id bigint not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        sender_id bigint not null,
        updated_at datetime(6),
        content varchar(1000) not null,
        primary key (id)
    ) engine=InnoDB;

    create table chat_rooms (
        buyer_id bigint not null,
        buyer_last_read_message_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        product_id bigint not null,
        seller_id bigint not null,
        seller_last_read_message_id bigint,
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table comments (
        created_at datetime(6),
        deleted_at datetime(6),
        id bigint not null auto_increment,
        member_id bigint not null,
        product_id bigint not null,
        updated_at datetime(6),
        content varchar(500) not null,
        primary key (id)
    ) engine=InnoDB;

    create table email_verifications (
        verified bit not null,
        code varchar(6) not null,
        created_at datetime(6),
        expires_at datetime(6) not null,
        id bigint not null auto_increment,
        sent_at datetime(6) not null,
        updated_at datetime(6),
        verified_at datetime(6),
        email varchar(100) not null,
        primary key (id)
    ) engine=InnoDB;

    create table favorites (
        created_at datetime(6),
        id bigint not null auto_increment,
        member_id bigint not null,
        product_id bigint not null,
        updated_at datetime(6),
        primary key (id)
    ) engine=InnoDB;

    create table member_agreements (
        agreed_at datetime(6) not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        member_id bigint not null,
        updated_at datetime(6),
        version varchar(20) not null,
        ip_address varchar(45),
        user_agent varchar(500),
        agreement_type enum ('PERSONAL_INFO_COLLECTION','TERMS_OF_SERVICE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table member_locations (
        active bit not null,
        sort_order integer not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        member_id bigint not null,
        updated_at datetime(6),
        region varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table members (
        created_at datetime(6),
        deleted_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        nickname varchar(20) not null,
        email varchar(100) not null,
        password varchar(100) not null,
        role enum ('ROLE_ADMIN','ROLE_USER') not null,
        status enum ('ACTIVE','DELETED','SUSPENDED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table notifications (
        is_read bit not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        last_notified_at datetime(6) not null,
        product_id bigint not null,
        recipient_id bigint not null,
        updated_at datetime(6),
        message varchar(500) not null,
        type enum ('COMMENT','PRICE_CHANGE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table password_reset_tokens (
        created_at datetime(6),
        expires_at datetime(6) not null,
        id bigint not null auto_increment,
        member_id bigint not null,
        sent_at datetime(6) not null,
        updated_at datetime(6),
        token_hash varchar(64) not null,
        primary key (id)
    ) engine=InnoDB;

    create table product_images (
        representative bit not null,
        sort_order integer not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        product_id bigint not null,
        updated_at datetime(6),
        image_url varchar(1000) not null,
        primary key (id)
    ) engine=InnoDB;

    create table products (
        favorite_count integer not null,
        hidden bit not null,
        price decimal(38,2) not null,
        category_id bigint not null,
        created_at datetime(6),
        deleted_at datetime(6),
        id bigint not null auto_increment,
        member_id bigint not null,
        updated_at datetime(6),
        view_count bigint not null,
        region varchar(100) not null,
        title varchar(100) not null,
        description varchar(1000) not null,
        thumbnail_url varchar(1000),
        trade_status enum ('COMPLETED','ON_SALE','RESERVED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table refresh_tokens (
        created_at datetime(6),
        expires_at datetime(6) not null,
        id bigint not null auto_increment,
        member_id bigint not null,
        updated_at datetime(6),
        token varchar(512) not null,
        primary key (id)
    ) engine=InnoDB;

    create table regions (
        id bigint not null auto_increment,
        name varchar(50) not null,
        primary key (id)
    ) engine=InnoDB;

    create table reports (
        created_at datetime(6),
        id bigint not null auto_increment,
        reporter_id bigint not null,
        target_member_id bigint,
        target_product_id bigint,
        updated_at datetime(6),
        content varchar(500),
        evidence_image_url varchar(500),
        reason enum ('ETC','FAKE_ITEM','FRAUD_SUSPECTED','INAPPROPRIATE_CONTENT','PROHIBITED_ITEM') not null,
        report_type enum ('MEMBER','PRODUCT') not null,
        status enum ('COMPLETED','RECEIVED','REJECTED','REVIEWING') not null,
        primary key (id)
    ) engine=InnoDB;

    alter table categories 
       add constraint UKt8o6pivur7nn124jehx7cygw5 unique (name);

    alter table chat_rooms 
       add constraint uk_chat_rooms_product_buyer unique (product_id, buyer_id);

    alter table email_verifications 
       add constraint UKjs5u5jd7gxiwv9c9ep08ryotb unique (email);

    alter table favorites 
       add constraint uk_favorites_member_product unique (member_id, product_id);

    alter table member_locations 
       add constraint uk_member_locations_member_region unique (member_id, region);

    alter table members 
       add constraint UKe6u9u9ypoc7oldnpxdjwcdx3 unique (nickname);

    alter table members 
       add constraint UK9d30a9u1qpg8eou0otgkwrp5d unique (email);

    create index idx_notifications_recipient 
       on notifications (recipient_id);

    alter table password_reset_tokens 
       add constraint UKdqhtiwoqb0v12kw0lkeai0ln unique (member_id);

    alter table password_reset_tokens 
       add constraint UKajre85ybxavf1tt4omkrs5p6g unique (token_hash);

    create index idx_products_region 
       on products (region);

    alter table refresh_tokens 
       add constraint UK6vmhlugnhc5iqbcdv1i5ebswn unique (member_id);

    alter table regions 
       add constraint UK1m9qnhbk56c8iskxvfupln9me unique (name);

    create index idx_reports_reporter_id 
       on reports (reporter_id);

    create index idx_reports_target_product_id 
       on reports (target_product_id);

    create index idx_reports_target_member_id 
       on reports (target_member_id);

    alter table reports 
       add constraint uk_reports_reporter_target_product unique (reporter_id, target_product_id);

    alter table reports 
       add constraint uk_reports_reporter_target_member unique (reporter_id, target_member_id);

    alter table chat_messages 
       add constraint FKbcsxusjp1v4rd8879fhvq8ssb 
       foreign key (chat_room_id) 
       references chat_rooms (id);

    alter table chat_messages 
       add constraint FKmf86klrrgnufxig1bgb94kafu 
       foreign key (sender_id) 
       references members (id);

    alter table chat_rooms 
       add constraint FKe0npbk8wfviu0giv2x6yp9hp7 
       foreign key (buyer_id) 
       references members (id);

    alter table chat_rooms 
       add constraint FKo52t6lfonn86xk7t8vapqkniv 
       foreign key (product_id) 
       references products (id);

    alter table chat_rooms 
       add constraint FKpakf4cn47kvwr8x4ovl9gcmc7 
       foreign key (seller_id) 
       references members (id);

    alter table comments 
       add constraint FKkv22t54g17a6hvj7hbn6byh5s 
       foreign key (member_id) 
       references members (id);

    alter table comments 
       add constraint FK6uv0qku8gsu6x1r2jkrtqwjtn 
       foreign key (product_id) 
       references products (id);

    alter table favorites 
       add constraint FK1rcmdug9ti11sd8ei528uyjid 
       foreign key (member_id) 
       references members (id);

    alter table favorites 
       add constraint FK6sgu5npe8ug4o42bf9j71x20c 
       foreign key (product_id) 
       references products (id);

    alter table member_agreements 
       add constraint FKin1k36b9irmhjm6nxmda0hjc7 
       foreign key (member_id) 
       references members (id);

    alter table member_locations 
       add constraint FKbiwca84nr51e0i66lbh922et 
       foreign key (member_id) 
       references members (id);

    alter table notifications 
       add constraint FKqu0hchk409ykhv450y9vkp7h8 
       foreign key (recipient_id) 
       references members (id);

    alter table product_images 
       add constraint FKqnq71xsohugpqwf3c9gxmsuy 
       foreign key (product_id) 
       references products (id);

    alter table products 
       add constraint FKog2rp4qthbtt2lfyhfo32lsw9 
       foreign key (category_id) 
       references categories (id);

    alter table products 
       add constraint FKacin2x19njxtk4k2pamwljcjv 
       foreign key (member_id) 
       references members (id);

    alter table reports 
       add constraint FKstudkqwdqpfvo0xkc8kg8e5fb 
       foreign key (reporter_id) 
       references members (id);

    alter table reports 
       add constraint FK1h01p2aso9nqhpt4twvtaag6j 
       foreign key (target_member_id) 
       references members (id);

    alter table reports 
       add constraint FKtpfswborrnu593i51ruh7bvwg 
       foreign key (target_product_id) 
       references products (id);

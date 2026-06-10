create table if not exists equipment_archive (
  id bigint primary key auto_increment,
  legacy_id bigint null,
  model varchar(255) not null,
  name varchar(255) not null,
  parameter text null,
  amount int not null default 0,
  supplier_id bigint null,
  purchase_time datetime null,
  region varchar(255) null,
  responsible_user_id bigint null,
  document varchar(500) null,
  remark text null,
  status varchar(30) not null default 'normal',
  created_at datetime not null,
  updated_at datetime null,
  deleted_at datetime null,
  unique key uk_equipment_archive_legacy (legacy_id)
);

create table if not exists equipment_item (
  id bigint primary key auto_increment,
  legacy_id bigint null,
  archive_id bigint not null,
  coding varchar(20) not null,
  equipment_code varchar(50) not null,
  work_status varchar(30) not null default 'normal',
  status varchar(30) not null default 'normal',
  created_at datetime not null,
  updated_at datetime null,
  deleted_at datetime null,
  unique key uk_equipment_item_coding (coding),
  unique key uk_equipment_item_legacy (legacy_id)
);

create table if not exists repair_order (
  id bigint primary key auto_increment,
  legacy_id bigint null,
  repair_code varchar(30) not null,
  archive_id bigint not null,
  equipment_id bigint not null,
  register_user_id bigint null,
  register_time datetime null,
  content text null,
  register_image varchar(500) null,
  repair_user_id bigint null,
  assign_time datetime null,
  repair_time datetime null,
  repair_content text null,
  repair_image varchar(500) null,
  failure_cause_id bigint null,
  consuming_seconds int not null default 0,
  status varchar(30) not null,
  created_at datetime not null,
  updated_at datetime null,
  deleted_at datetime null,
  unique key uk_repair_order_code (repair_code),
  unique key uk_repair_order_legacy (legacy_id)
);

create table if not exists plan_task (
  id bigint primary key auto_increment,
  legacy_id bigint null,
  coding varchar(20) not null,
  plan_id bigint not null,
  equipment_id bigint not null,
  task_user_id bigint null,
  type varchar(30) not null,
  status varchar(30) not null default 'pending',
  start_time datetime null,
  due_time datetime null,
  created_at datetime not null,
  updated_at datetime null,
  deleted_at datetime null,
  unique key uk_plan_task_coding (coding),
  unique key uk_plan_task_legacy (legacy_id)
);

create table if not exists equipment_record (
  id bigint primary key auto_increment,
  legacy_id bigint null,
  equipment_id bigint not null,
  relate_id bigint null,
  add_user_id bigint null,
  name varchar(128) not null,
  type varchar(64) not null,
  content text null,
  status varchar(30) not null default 'normal',
  created_at datetime not null,
  updated_at datetime null,
  deleted_at datetime null,
  unique key uk_equipment_record_legacy (legacy_id)
);

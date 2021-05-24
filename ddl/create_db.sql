
load classes ../lib/gson-2.2.2.jar;

load classes ../jars/voltdb-cvmsandbox.jar;

file -inlinebatch END_OF_BATCH

CREATE table user_table
(userid bigint not null primary key
,user_json_object varchar(8000)
,user_last_seen TIMESTAMP DEFAULT NOW
,user_softlock_sessionid bigint 
,user_softlock_expiry TIMESTAMP);

PARTITION TABLE user_table ON COLUMN userid;

create index ut_del on user_table(user_last_seen);

create index ut_loyaltycard on user_table (field(user_json_object, 'loyaltySchemeNumber'));

CREATE table user_cvm_offers
(userid bigint not null
,offer_type    varchar(30) not null
,offer_date TIMESTAMP not null
,offer_payload varchar(8000)
,primary key (userid,offer_date,offer_type))
USING TTL 180 DAYS ON COLUMN offer_date;

CREATE INDEX cvm_ix1 ON user_cvm_offers (offer_date);

CREATE INDEX cvm_ix2 ON user_cvm_offers (offer_type
     , TRUNCATE(MINUTE,offer_date));

PARTITION TABLE user_cvm_offers ON COLUMN userid;

CREATE VIEW cvm_offer_activity AS
SELECT offer_type
     , TRUNCATE(MINUTE,offer_date) offer_date
     , min(userid) min_userid
     , count(*) how_many
FROM user_cvm_offers
GROUP BY offer_type, TRUNCATE(MINUTE,offer_date);

create table user_usage_table
(userid bigint not null
,allocated_amount bigint not null
,sessionid bigint  not null
,lastdate timestamp not null
,primary key (userid, sessionid))
USING TTL 180 MINUTES ON COLUMN lastdate;

CREATE INDEX ust_del_idx1 ON user_usage_table(lastdate);

PARTITION TABLE user_usage_table ON COLUMN userid;

CREATE INDEX uut_ix1 ON user_usage_table(userid, lastdate);

create table user_recent_transactions
(userid bigint not null 
,user_txn_id varchar(128) NOT NULL
,txn_time TIMESTAMP DEFAULT NOW  not null 
,sessionid bigint
,approved_amount bigint 
,spent_amount bigint 
,purpose  varchar(128)
,primary key (userid, user_txn_id))
USING TTL 180 MINUTES ON COLUMN txn_time;

PARTITION TABLE user_recent_transactions ON COLUMN userid;

CREATE INDEX urt_idx1 ON user_recent_transactions(userid, txn_time) ;

CREATE INDEX urt_del_idx4 ON user_recent_transactions(txn_time);

CREATE INDEX urt_del_idx5 ON user_recent_transactions (TRUNCATE(MINUTE,txn_time));

-- Used by recent_activity_in when rows are deleted...
CREATE INDEX urt_del_idx6 ON user_recent_transactions (TRUNCATE(MINUTE,txn_time)) WHERE spent_amount > 0;

-- Used by recent_activity_out when rows are deleted...
CREATE INDEX urt_del_idx7 ON user_recent_transactions (TRUNCATE(MINUTE,txn_time)) WHERE spent_amount <= 0;

CREATE STREAM user_financial_events 
EXPORT TO TOPIC user_financial_events 
WITH KEY (userid)
PARTITION on column userid
(userid bigint not null 
,amount bigint not null
,user_txn_id varchar(128) not null
,message varchar(80) not null);

create view current_locks as
select count(*) how_many 
from user_table 
where user_softlock_expiry is not null;

create view user_balance as
select userid, sum(amount) balance
from user_financial_events
group by userid;

create view allocated_credit as 
select sum(allocated_amount) allocated_amount 
from user_usage_table;

create view users_sessions as 
select userid,  sessionid, count(*) how_many 
from user_usage_table
group by  userid,  sessionid;

create index uss_ix1 on users_sessions (how_many) WHERE how_many > 1;

create view recent_activity_out as
select TRUNCATE(MINUTE,txn_time) txn_time
       , sum(approved_amount * -1) approved_amount
       , sum(spent_amount) spent_amount
       , count(*) how_many
from user_recent_transactions
where spent_amount <= 0
GROUP BY TRUNCATE(MINUTE,txn_time) ;

create view recent_activity_in as
select TRUNCATE(MINUTE,txn_time) txn_time
       , sum(approved_amount) approved_amount
       , sum(spent_amount) spent_amount
       , count(*) how_many
from user_recent_transactions
where spent_amount > 0
GROUP BY TRUNCATE(MINUTE,txn_time) ;


create view cluster_activity_by_users as 
select userid,  count(*) how_many
from user_recent_transactions
group by userid;

create view cluster_activity as 
select truncate(minute, txn_time) txn_time, count(*) how_many
from user_recent_transactions
group by truncate(minute, txn_time) ;

create view last_cluster_activity as 
select  max(txn_time) txn_time
from user_recent_transactions;

create view cluster_users as 
select  count(*) how_many
from user_table;

create procedure GetUsersWithMultipleSessions
AS 
SELECT * FROM users_sessions 
WHERE how_many > 1
ORDER BY how_many, userid, sessionid
LIMIT 50;

create procedure showTransactions
PARTITION ON TABLE user_table COLUMN userid
as 
select * from user_recent_transactions where userid = ? ORDER BY txn_time, user_txn_id;


create procedure FindByLoyaltyCard as select * from user_table where field(user_json_object, 'loyaltySchemeNumber') = CAST(? AS VARCHAR);



CREATE PROCEDURE ShowCurrentAllocations__promBL AS
BEGIN
select 'user_locks' statname,  'user_locks' stathelp  ,how_many statvalue from current_locks;
select 'user_count' statname,  'user_count' stathelp  ,how_many statvalue from cluster_users;
select 'allocated_credit' statname,  'allocated_credit' stathelp  ,allocated_amount statvalue from allocated_credit;
select 'recent_activity_out_approved' statname
     , 'recent_activity_out_approved' stathelp  
     , approved_amount statvalue 
from recent_activity_out where txn_time = truncate(minute, DATEADD(MINUTE, -1, NOW));
select 'recent_activity_out_spent' statname
     , 'recent_activity_out_spent' stathelp  
     , spent_amount statvalue 
from recent_activity_out where txn_time = truncate(minute, DATEADD(MINUTE, -1, NOW));
select 'recent_activity_out_qty' statname
     , 'recent_activity_out_qty' stathelp  
     , how_many statvalue 
from recent_activity_out where txn_time = truncate(minute, DATEADD(MINUTE, -1, NOW));
select 'recent_activity_in_spent' statname
     , 'recent_activity_in_spent' stathelp  
     , spent_amount statvalue 
from recent_activity_in where txn_time = truncate(minute, DATEADD(MINUTE, -1, NOW));
select 'recent_activity_in_qty' statname
     , 'recent_activity_in_qty' stathelp  
     , how_many statvalue 
from recent_activity_in where txn_time = truncate(minute, DATEADD(MINUTE, -1, NOW));

SELECT 'cvm_offers_'||offer_type statname,
'cvm_offers_'||offer_type stathelp,
how_many statvalue
FROM cvm_offer_activity
WHERE  offer_date = truncate(minute, DATEADD(MINUTE, -1, NOW));

END;


CREATE PROCEDURE 
   PARTITION ON TABLE user_table COLUMN userid
   FROM CLASS chargingdemoprocs.GetUser;
   
CREATE PROCEDURE 
   PARTITION ON TABLE user_table COLUMN userid
   FROM CLASS chargingdemoprocs.GetAndLockUser;
   
CREATE PROCEDURE 
   PARTITION ON TABLE user_table COLUMN userid
   FROM CLASS chargingdemoprocs.UpdateLockedUser;
   
CREATE PROCEDURE 
   PARTITION ON TABLE user_table COLUMN userid
   FROM CLASS chargingdemoprocs.UpsertUser;
   
CREATE PROCEDURE 
   PARTITION ON TABLE user_table COLUMN userid
   FROM CLASS chargingdemoprocs.DelUser;
   
CREATE PROCEDURE 
   PARTITION ON TABLE user_table COLUMN userid
   FROM CLASS chargingdemoprocs.ReportQuotaUsage;  
   
CREATE PROCEDURE 
   PARTITION ON TABLE user_table COLUMN userid
   FROM CLASS chargingdemoprocs.AddCredit;  


END_OF_BATCH

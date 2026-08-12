INSERT INTO subscribers(operator_id, msisdn, name, status)
select 'acme',
       9876543 || lpad(i::text,3,'0'),
       'load test subscriber: ' || i,
       'ACTIVE'
from generate_series(0,999) as i
on conflict(operator_id, msisdn) do nothing;


insert into subscriber_plans(subscriber_id, plan_id , active)
select s.id,1 , true
from subscribers s
where s.operator_id='acme' and s.msisdn between '9876543000' and '9876543999'
and not exists(select 1 from subscriber_plans sp where sp.subscriber_id=s.id and sp.active=true);

insert into balances(subscriber_id , amount , version)
select s.id, 1000000.0000, 0
from subscribers s
where s.operator_id='acme' and s.msisdn between '9876543000' and '9876543999'
on conflict (subscriber_id) do update set amount = 1000000.0000;


-- Get activities of the users
select u.email,
       o.name,
       c.id,
       c.is_finished,
       sk.name,
       c.simulation_id,
       s.name,
       c.timestamp
from users u
         left join organizations o on o.id = u.organization_id
         right join chats c on c.user_id = u.id
         left join simulations s on s.id = c.simulation_id
         left join skills sk on sk.id = c.skill_id
where o.name <> 'Soft Trainer'
  and u.department <> 'SoftTrainer';


-- get the values of hyper params for the first finished chat and its total values
WITH first_completed_chats AS (
    SELECT
        c.id as chat_id,
        c.user_id,
        c.simulation_id,
        c.timestamp,
        ROW_NUMBER() OVER (PARTITION BY c.user_id, c.simulation_id ORDER BY c.timestamp) as chat_order
    FROM
        chats c
    WHERE
        c.is_finished = true
), individual_results AS (

    SELECT
        us.email,
        fcc.chat_id,
        fcc.simulation_id,
        s.name as simulation_name,
        fuh.tension_reduction,
        fuh.clarity_politeness,
        fuh.problem_solving,
        fuh.support_trust,
        0 as is_total
    FROM
        fixed_user_hyperparams fuh
            INNER JOIN first_completed_chats fcc ON fuh.chat_id = fcc.chat_id AND fcc.chat_order = 1
            INNER JOIN users us ON us.id = fcc.user_id
            INNER JOIN simulations s ON s.id = fcc.simulation_id
            INNER JOIN organizations o ON o.id = us.organization_id
            INNER JOIN skills sk ON sk.id = s.skill_id
    WHERE
        sk.id = 502
      AND o.id = 24
), user_totals AS (
    SELECT
        email,
        0 as chat_id,  -- explicit cast for bigint
        0 as simulation_id,  -- explicit cast for bigint
        'TOTAL' as simulation_name,
        SUM(tension_reduction) as tension_reduction,
        SUM(clarity_politeness) as clarity_politeness,
        SUM(problem_solving) as problem_solving,
        SUM(support_trust) as support_trust,
        1 as is_total
    FROM
        individual_results
    GROUP BY
        email
)
SELECT * FROM individual_results
UNION ALL
SELECT * FROM user_totals
ORDER BY
    email,
    is_total;



-- count mistakes per chat
SELECT
    u.email,
    s.name as simulation_name,
    s.id,
    c.id as chat_id,
    COUNT(m.id) as total_messages_in_chat,
    COUNT(m.id) - count(DISTINCT n.order_number) as number_failed_per_chat
FROM
    chats c
        inner join users u on u.id = c.user_id
        inner join organizations o on o.id = u.organization_id
        inner join simulations s on s.id = c.simulation_id
        inner join messages m on c.id = m.chat_id
        inner join nodes n on n.id = m.flow_node_id
        inner join skills sk on sk.id = s.skill_id
where o.id = 24 and sk.id  = 502
GROUP BY
    u.email,
    s.name,
    c.id,
    s.id
ORDER BY
    u.email,
    s.name,
    c.id;


 -- count mistakes per simulation for the user for all chats
SELECT
    u.email AS user_email,
    s.name AS simulation_name,
    COUNT(c.id) AS attempts_per_simulation,
    (COALESCE(SUM(s.hearts), 0) - COALESCE(SUM(c.hearts), 0)) AS losed_hearts
FROM
    users u
        INNER JOIN organizations o ON o.id = u.organization_id
        INNER JOIN organizations_skills os ON os.organization_id = o.id
        INNER JOIN skills sk ON sk.id = os.skill_id
        INNER JOIN skills_simulations ss ON ss.skills_id = sk.id
        INNER JOIN simulations s ON s.id = ss.simulations_key
        left JOIN chats c ON  c.simulation_id = s.id and u.id = c.user_id -- Ensure chats match simulations
WHERE
    o.id = 24 -- Replace with the desired organization ID
  AND sk.id = 502 -- Replace with the desired skill ID
GROUP BY
    u.email,
    s.name,
    s.hearts
ORDER BY
    u.email,
    s.name;


WITH first_chat_per_user AS (
    SELECT
        c.user_id,
        c.simulation_id,
        MIN(c.timestamp) AS first_chat_timestamp
    FROM
        chats c
    GROUP BY
        c.user_id, c.simulation_id
)
SELECT
    u.email AS user_email,
    s.name AS simulation_name,
    COUNT(c.id) AS attempted_chats_per_simulation, -- Count the user's total chats for the simulation
    (COALESCE(s.hearts, 0) - COALESCE(c.hearts, 0)) AS losed_hearts, -- Calculate lost hearts for the first chat only
    c.is_finished
FROM
    users u
        INNER JOIN organizations o ON o.id = u.organization_id
        INNER JOIN organizations_skills os ON os.organization_id = o.id
        INNER JOIN skills sk ON sk.id = os.skill_id
        INNER JOIN skills_simulations ss ON ss.skills_id = sk.id
        INNER JOIN simulations s ON s.id = ss.simulations_key
        LEFT JOIN first_chat_per_user fc ON fc.user_id = u.id AND fc.simulation_id = s.id
        LEFT JOIN chats c ON c.simulation_id = fc.simulation_id AND c.timestamp = fc.first_chat_timestamp
WHERE
    o.id = 24 -- Replace with the desired organization ID
  AND sk.id = 502 -- Replace with the desired skill ID
GROUP BY
    u.email,
    s.name,
    s.hearts,
    c.id,
    c.is_finished -- Include this in GROUP BY since it is used in the SELECT
ORDER BY
    u.email,
    s.name;


-- count mistakes per first chat of simulation, status of completion and lost hearts
WITH first_chat_per_user AS (
    SELECT
        c.user_id,
        c.simulation_id,
        MIN(c.timestamp) AS first_chat_timestamp
    FROM
        chats c
    GROUP BY
        c.user_id, c.simulation_id
)
SELECT
    u.email AS user_email,
    s.name AS simulation_name,
    COUNT(c.id) AS attempted_chats_per_simulation, -- Count the user's total chats for the simulation
    (COALESCE(s.hearts, 0) - COALESCE(c.hearts, 0)) AS losed_hearts, -- Calculate lost hearts for the first chat only
    c.is_finished
FROM
    users u
        INNER JOIN organizations o ON o.id = u.organization_id
        INNER JOIN organizations_skills os ON os.organization_id = o.id
        INNER JOIN skills sk ON sk.id = os.skill_id
        INNER JOIN skills_simulations ss ON ss.skills_id = sk.id
        INNER JOIN simulations s ON s.id = ss.simulations_key
        LEFT JOIN first_chat_per_user fc ON fc.user_id = u.id AND fc.simulation_id = s.id
        LEFT JOIN chats c ON c.simulation_id = fc.simulation_id AND c.timestamp = fc.first_chat_timestamp
WHERE
    o.id = 24 -- Replace with the desired organization ID
  AND sk.id = 502 -- Replace with the desired skill ID
GROUP BY
    u.email,
    s.name,
    s.hearts,
    c.id,
    c.is_finished -- Include this in GROUP BY since it is used in the SELECT
ORDER BY
    u.email,
    s.name;




-- count mistakes per skill for user with the first chat, lost hearts
WITH first_chat_per_user AS (
    SELECT
        c.user_id,
        c.simulation_id,
        MIN(c.timestamp) AS first_chat_timestamp
    FROM
        chats c
    GROUP BY
        c.user_id, c.simulation_id
),
     first_chats_details AS (
         SELECT
             u.email AS user_email,
             sk.name AS skill_name,
             COUNT(c.id) AS attempted_chats_per_simulation, -- Count the user's total chats for the simulation
             (COALESCE(s.hearts, 0) - COALESCE(c.hearts, 0)) AS losed_hearts, -- Calculate lost hearts for the first chat only
             c.is_finished
         FROM
             users u
                 INNER JOIN organizations o ON o.id = u.organization_id
                 INNER JOIN organizations_skills os ON os.organization_id = o.id
                 INNER JOIN skills sk ON sk.id = os.skill_id
                 INNER JOIN skills_simulations ss ON ss.skills_id = sk.id
                 INNER JOIN simulations s ON s.id = ss.simulations_key
                 LEFT JOIN first_chat_per_user fc ON fc.user_id = u.id AND fc.simulation_id = s.id
                 LEFT JOIN chats c ON c.simulation_id = fc.simulation_id AND c.timestamp = fc.first_chat_timestamp
         WHERE
             o.id = 24 -- Replace with the desired organization ID
           AND sk.id = 502 -- Replace with the desired skill ID
         GROUP BY
             u.email,
             sk.name,
             s.hearts,
             c.id,
             c.is_finished -- Include this in GROUP BY since it is used in the SELECT
     )
SELECT
    user_email,
    skill_name,
    SUM(attempted_chats_per_simulation) AS total_attempted_chats,
    SUM(losed_hearts) AS total_losed_hearts
FROM
    first_chats_details
GROUP BY
    user_email,
    skill_name
ORDER BY
    user_email,
    skill_name;


-- count attempts(chats) per simulation of user, status of completion and lost hearts
SELECT
    u.email AS user_email,
    s.name AS simulation_name,
    COUNT(c.id) AS attempted_chats_per_simulation -- Count the user's total chats for the simulation
FROM
    users u
        INNER JOIN organizations o ON o.id = u.organization_id
        INNER JOIN organizations_skills os ON os.organization_id = o.id
        INNER JOIN skills sk ON sk.id = os.skill_id
        INNER JOIN skills_simulations ss ON ss.skills_id = sk.id
        INNER JOIN simulations s ON s.id = ss.simulations_key
        LEFT JOIN chats c ON c.simulation_id = s.id and c.user_id = u.id
WHERE
    o.id = 24 -- Replace with the desired organization ID
  AND sk.id = 502 -- Replace with the desired skill ID
GROUP BY
    u.email,
    s.name-- Include this in GROUP BY since it is used in the SELECT
ORDER BY
    u.email,
    s.name;

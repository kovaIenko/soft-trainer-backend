-- Fix column length constraints for message answer fields
-- These fields were defaulting to varchar(255) but need to store longer AI classification responses
-- All message types use single table inheritance and are stored in the 'messages' table

-- Check which columns exist in the messages table and update them if they need longer lengths
DO $$
BEGIN
    -- Update answer column if it exists and is too short
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'messages' 
               AND column_name = 'answer' 
               AND character_maximum_length < 2000) THEN
        ALTER TABLE messages ALTER COLUMN answer TYPE varchar(2000);
        RAISE NOTICE 'Updated messages.answer column to varchar(2000)';
    END IF;

    -- Update open_answer column if it exists and is too short  
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'messages' 
               AND column_name = 'open_answer' 
               AND character_maximum_length < 2000) THEN
        ALTER TABLE messages ALTER COLUMN open_answer TYPE varchar(2000);
        RAISE NOTICE 'Updated messages.open_answer column to varchar(2000)';
    END IF;

    -- Update openanswer column if it exists (alternative naming)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'messages' 
               AND column_name = 'openanswer' 
               AND character_maximum_length < 2000) THEN
        ALTER TABLE messages ALTER COLUMN openanswer TYPE varchar(2000);
        RAISE NOTICE 'Updated messages.openanswer column to varchar(2000)';
    END IF;

    -- Update preview column if it exists and is too short
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'messages' 
               AND column_name = 'preview' 
               AND character_maximum_length < 500) THEN
        ALTER TABLE messages ALTER COLUMN preview TYPE varchar(500);
        RAISE NOTICE 'Updated messages.preview column to varchar(500)';
    END IF;

    -- Update options column if it exists and is too short (may contain long JSON)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'messages' 
               AND column_name = 'options' 
               AND character_maximum_length < 2000) THEN
        ALTER TABLE messages ALTER COLUMN options TYPE varchar(2000);
        RAISE NOTICE 'Updated messages.options column to varchar(2000)';
    END IF;

    -- Update correct column if it exists and is too short
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'messages' 
               AND column_name = 'correct' 
               AND character_maximum_length < 1000) THEN
        ALTER TABLE messages ALTER COLUMN correct TYPE varchar(1000);
        RAISE NOTICE 'Updated messages.correct column to varchar(1000)';
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        -- Log the error but don't fail the migration
        RAISE NOTICE 'Warning: Some column updates may have failed: %', SQLERRM;
END $$;

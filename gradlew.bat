@echo off
echo ========================================
echo Pushing Game Catalog to GitHub...
echo ========================================

cd /d C:\Users\Administrator\AndroidStudioProjects\GameCatalog

echo Adding all files...
git add .

echo Creating commit...
git commit -m "Update: %date% %time%"

echo Pushing to GitHub...
git push --force

echo ========================================
echo Done!
echo ========================================
pause
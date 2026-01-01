$jars = Get-ChildItem -Path "C:\Users\Tim\.gradle\caches\fabric-loom" -Recurse -Filter "*.jar"
foreach ($jar in $jars) {
    $out = javap -p -cp $jar.FullName net.minecraft.screen.AbstractCraftingScreenHandler 2>&1
    if ($out -notmatch "class not found") {
        Write-Host "Found AbstractRecipeScreenHandler in: $($jar.FullName)"
        Write-Host $out
        break
    }
}
import csv
import sqlite3
import os

def creer_base_par_composante():
    """Crée une base de données SQLite pour chaque composante"""
    chemin_csv = '/Users/M/Downloads/classes_filtrees.csv'
    
    # Dossier pour les bases de données
    dossier_bases = 'bases_composantes'
    os.makedirs(dossier_bases, exist_ok=True)
    
    try:
        with open(chemin_csv, 'r', encoding='utf-8-sig') as fichier:
            lecteur = csv.DictReader(fichier, delimiter=';')
            
            # Nettoyer les noms de colonnes
            noms_colonnes_propres = [col.strip().replace('\ufeff', '') for col in lecteur.fieldnames]
            lecteur.fieldnames = noms_colonnes_propres
            
            print(f"Colonnes détectées: {lecteur.fieldnames}")
            
            # Dictionnaire pour stocker les salles par composante
            salles_par_composante = {}
            
            for ligne in lecteur:
                code = ligne['Code'].strip() if ligne['Code'] else ''
                nom_salle = ligne['Nom'].strip() if ligne['Nom'] else ''
                composante = ligne['Composante'].strip() if ligne['Composante'] else ''
                
                # Ignorer les lignes vides
                if not code and not nom_salle and not composante:
                    continue
                
                # Gérer les composantes multiples séparées par des virgules
                composantes = [comp.strip() for comp in composante.split(',')]
                
                for comp in composantes:
                    if comp:  # Ignorer les composantes vides
                        if comp not in salles_par_composante:
                            salles_par_composante[comp] = []
                        
                        salles_par_composante[comp].append({
                            'nom_salle': nom_salle,
                            'composante': comp
                        })
            
            # Créer une base de données pour chaque composante
            total_salles = 0
            for composante, salles in salles_par_composante.items():
                nom_fichier_db = f"{composante.replace(' ', '_').replace('/', '_').replace(',', '_')}.db"
                chemin_db = os.path.join(dossier_bases, nom_fichier_db)
                
                conn = sqlite3.connect(chemin_db)
                cursor = conn.cursor()
                
                # Création de la table sans la colonne 'code' , ajouter etage et description et modifier longitude et latitude ici quand on a le nouveau csv que anis va donner
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS salles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nom_salle TEXT NOT NULL,
                        composante TEXT NOT NULL,
                        longitude REAL DEFAULT 0,
                        latitude REAL DEFAULT 0
                    )
                ''')
                
                # Insérer les salles
                for salle in salles:
                    cursor.execute(
                        'INSERT INTO salles (nom_salle, composante, longitude, latitude) VALUES (?, ?, ?, ?)',
                        (salle['nom_salle'], salle['composante'], 0, 0)
                    )
                
                conn.commit()
                conn.close()
                
                print(f"✓ {composante}: {chemin_db} ({len(salles)} salles)")
                total_salles += len(salles)
            
            print(f"\nTotal: {len(salles_par_composante)} bases de données créées")
            print(f"Total des salles réparties: {total_salles}")
            
            return salles_par_composante
        
    except FileNotFoundError:
        print(f"Erreur: Le fichier {chemin_csv} n'a pas été trouvé")
    except Exception as e:
        print(f"Erreur lors de l'importation: {e}")
        import traceback
        traceback.print_exc()

def afficher_statistiques_bases():
    """Affiche des statistiques sur les bases de données créées"""
    dossier_bases = 'bases_composantes'
    
    if not os.path.exists(dossier_bases):
        print("Aucune base de données trouvée.")
        return
    
    bases = [f for f in os.listdir(dossier_bases) if f.endswith('.db')]
    
    if not bases:
        print("Aucune base de données trouvée.")
        return
    
    print(f"\n=== STATISTIQUES DES BASES DE DONNÉES ===")
    print(f"Nombre de bases créées: {len(bases)}")
    
    total_salles_toutes_bases = 0
    for base in sorted(bases):
        chemin_base = os.path.join(dossier_bases, base)
        conn = sqlite3.connect(chemin_base)
        cursor = conn.cursor()
        
        cursor.execute('SELECT COUNT(*) FROM salles')
        nb_salles = cursor.fetchone()[0]
        
        cursor.execute('SELECT composante FROM salles LIMIT 1')
        composante = cursor.fetchone()[0]
        
        conn.close()
        
        print(f"  - {base}: {nb_salles} salles ({composante})")
        total_salles_toutes_bases += nb_salles
    
    print(f"Total des salles dans toutes les bases: {total_salles_toutes_bases}")

def afficher_premieres_lignes_base(composante):
    """Affiche les premières lignes d'une base de données spécifique"""
    dossier_bases = 'bases_composantes'
    nom_fichier_db = f"{composante.replace(' ', '_').replace('/', '_').replace(',', '_')}.db"
    chemin_db = os.path.join(dossier_bases, nom_fichier_db)
    
    if not os.path.exists(chemin_db):
        print(f"Base de données {composante} non trouvée")
        return
    
    conn = sqlite3.connect(chemin_db)
    cursor = conn.cursor()
    
    cursor.execute('SELECT * FROM salles LIMIT 5')
    lignes = cursor.fetchall()
    
    print(f"\n=== PREMIÈRES LIGNES DE {composante} ===")
    print("ID | Nom | Composante | Longitude | Latitude")
    print("-" * 50)
    for ligne in lignes:
        print(ligne)
    
    conn.close()

def main():
    """Fonction principale"""
    print("=== CRÉATION DE BASES DE DONNÉES PAR COMPOSANTE ===")
    print("Chemin du fichier CSV: /Users/M/Downloads/Salles_univ.csv")
    print("Séparateur: point-virgule (;)\n")
    
    # Créer les bases de données par composante
    salles_par_composante = creer_base_par_composante()
    
    # Afficher les statistiques
    afficher_statistiques_bases()
    
    # Afficher un exemple pour une composante
    if salles_par_composante:
        premiere_composante = list(salles_par_composante.keys())[0]
        afficher_premieres_lignes_base(premiere_composante)
    
    print(f"\n✅ Opération terminée!")
    print(f"📁 Toutes les bases de données sont dans le dossier 'bases_composantes'")

if __name__ == "__main__":
    main()
